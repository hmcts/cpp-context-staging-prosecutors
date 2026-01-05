package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.util.Collections.singletonList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission.poller;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CPS_SERVE_BCM;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CPS_SERVE_COTR;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CPS_SERVE_PET;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CPS_SERVE_PTPH;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CPS_UPDATE_COTR;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.WRITE_BASE_URI_BCM;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.WRITE_BASE_URI_COTR;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.WRITE_BASE_URI_PET;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.WRITE_BASE_URI_PTPH;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.WRITE_BASE_URI_UPDATE_COTR;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.fetchSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.importServeMaterialCaseAndWaitUntilReady;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicProgressionSubmissionAndWaitForProcessing;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendPublicEventToUpdateSubmissionStatus;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.TopicUtils.retrieveMessageAsString;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS_WITH_WARNINGS;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ApplicationsForDirectionsGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.AssociatedPerson;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsBcmReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPetReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defence;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Prosecution;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(JmsResourceManagementExtension.class)
public class CpsServeMaterialIT {

    protected JmsMessageConsumerClient cpsServePetReceivedEventsConsumer;
    protected JmsMessageConsumerClient cpsServeBcmReceivedEventsConsumer;
    protected JmsMessageConsumerClient cpsServePtphReceivedEventsConsumer;
    protected JmsMessageConsumerClient cpsServeCotrReceivedEventsConsumer;
    protected JmsMessageConsumerClient cpsUpdateCotrReceivedEventsConsumer;
    protected JmsMessageConsumerClient cpsUpdateCotrReviewNotesEventsConsumer;

    private static final String PUBLIC_EVENT_PET_RECEIVED = "public.stagingprosecutors.cps-serve-pet-received";
    private static final String PUBLIC_EVENT_BCM_RECEIVED = "public.stagingprosecutors.cps-serve-bcm-received";
    private static final String PUBLIC_EVENT_PTPH_RECEIVED = "public.stagingprosecutors.cps-serve-ptph-received";
    private static final String PUBLIC_EVENT_COTR_RECEIVED = "public.stagingprosecutors.cps-serve-cotr-received";
    private static final String PUBLIC_EVENT_UPDATE_COTR_RECEIVED = "public.stagingprosecutors.cps-update-cotr-received";
    private static final String PUBLIC_EVENT_REVIEW_NOTES_UPDATED = "public.progression.cotr-review-notes-updated";

    private static final String EVENT_FORM_CREATED = "public.progression.form-created";
    private static final String EVENT_PET_FORM_CREATED = "public.progression.pet-form-created";
    private static final String EVENT_COTR_CREATED = "public.progression.cotr-created";
    private static final String EVENT_FORM_OPERATION_FAILED = "public.progression.form-operation-failed";
    private static final String EVENT_COTR_OPERATION_FAILED = "public.progression.cotr-operation-failed";

    private static final String EVENT_COTR_REVIEW_NOTES_UPDATED = "public.progression.cotr-review-notes-updated";

    private static final String MEASURE_DETAILS = "measureDetails";
    private static final String N = "N";
    private static final String Y = "Y";
    private static final String PROSECUTOR_DEFENDANT_ID = "defendant Id";
    private static final String OFFENCE_CODE = "code123";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final String OFFENCE_DATE = "2021-09-27";
    private static final String PROSECUTION_AUTHORITY = "OU CODE";
    private static final String URN = "caseURN";
    private static final String NO_COMPLIANCE_DETAILS = "noComplianceDetails";
    private static final String DETAILS = "Details";
    private static final String SLAVERY_DETAILS = "SlaveryDetails";
    private static final String EQUIPMENT_DETAILS = "EquipmentDetails";
    private static final String LAW_DETAILS = "LawDetails";
    private static final String FORE_NAME = "forename";
    private static final String WELSH = "welsh";
    private static final String LAST_NAME = "abc";
    public static final String DIRECTION_DETAILS = "directionDetails";
    public static final String PHONE = "8778888345";
    public static final String EMAIL = "test@test.com";
    private static final String NAME = "name";

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    public void shouldSubmitCpsServePetInPendingStatus() {
        this.cpsServePetReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PET_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsServePetReceivedEventsConsumer);
        String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_PET_RECEIVED));

        final CpsPetReceivedDetails cpsPetReceivedDetails = jsonObjectToObjectConverter.convert(jsonEnvelope.get().payloadAsJsonObject(), CpsPetReceivedDetails.class);
        verifyPetReceivedPublicEvent(cpsPetReceivedDetails, submissionId);
    }

    @Test
    public void shouldRaiseBadRequestWhenSubmitCpsServePet() {
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet-bad-request.json", CPS_SERVE_PET)) {
            assertThat(BAD_REQUEST.getStatusCode(), is(response.getStatus()));
        }
    }

    @Test
    public void shouldSubmitCpsServeBcmInPendingStatus() {
        this.cpsServeBcmReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_BCM_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_BCM,
                "importCase/stagingprosecutors.cps-serve-bcm.json", CPS_SERVE_BCM)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsServeBcmReceivedEventsConsumer);
        String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_BCM_RECEIVED));
        final CpsBcmReceivedDetails cpsBcmReceivedDetails = jsonObjectToObjectConverter.convert(jsonEnvelope.get().payloadAsJsonObject(), CpsBcmReceivedDetails.class);
        verifyBcmReceivedPublicEvent(cpsBcmReceivedDetails, submissionId);
    }

    @Test
    public void shouldRaiseBadRequestWhenSubmitCpsServeBcm() {
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_BCM,
                "importCase/stagingprosecutors.cps-serve-bcm-bad-request.json", CPS_SERVE_BCM)) {
            assertThat(BAD_REQUEST.getStatusCode(), is(response.getStatus()));
        }
    }

    @Test
    public void shouldSubmitCpsServePtphInPendingStatusWithAllFields() throws Exception {
        this.cpsServePtphReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PTPH_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PTPH,
                "importCase/stagingprosecutors.cps-serve-ptph.json", CPS_SERVE_PTPH)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }
        UUID submissionId = fetchSubmissionId(urlResponse);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsServePtphReceivedEventsConsumer);
        String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_PTPH_RECEIVED));

        String actualPublicEvent = jsonEnvelope.get().payloadAsJsonObject().toString();
        String expectedPublicEvent = FileUtil.resourceToString("expected/public.stagingprosecutors.cps-serve-ptph-received.json")
                .replace("{SUBMISSION_ID}", submissionId.toString());

        JSONAssert.assertEquals(expectedPublicEvent, actualPublicEvent, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("*.matchingId", (o1, o2) -> true)
        ));
    }

    @Test
    public void shouldSubmitCpsServePtphInPendingStatusWithMandatoryFields() throws Exception {
        this.cpsServePtphReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PTPH_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PTPH,
                "importCase/stagingprosecutors.cps-serve-ptph-mandatory-fields.json", CPS_SERVE_PTPH)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsServePtphReceivedEventsConsumer);
        String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_PTPH_RECEIVED));

        String actualPublicEvent = jsonEnvelope.get().payloadAsJsonObject().toString();
        String expectedPublicEvent = FileUtil.resourceToString("expected/public.stagingprosecutors.cps-serve-ptph-received-mandatory-fields.json")
                .replace("{SUBMISSION_ID}", submissionId.toString());

        JSONAssert.assertEquals(expectedPublicEvent, actualPublicEvent, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("*.matchingId", (o1, o2) -> true)
        ));
    }

    @Test
    public void shouldRaiseBadRequestWhenSubmitCpsServePtph() {
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PTPH,
                "importCase/stagingprosecutors.cps-serve-ptph-bad-request.json", CPS_SERVE_PTPH)) {
            assertThat(BAD_REQUEST.getStatusCode(), is(response.getStatus()));
        }
    }

    @Test
    public void shouldSubmitCpsUpdateCotrInPendingStatus() {
        this.cpsUpdateCotrReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_UPDATE_COTR_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_UPDATE_COTR + "/7e2f843e-d639-40b3-8611-8015f3a18958",
                "importCase/stagingprosecutors.cps-update-cotr.json", CPS_UPDATE_COTR)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsUpdateCotrReceivedEventsConsumer);
        String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_UPDATE_COTR_RECEIVED));

        final JsonObject jsonObject = jsonEnvelope.get().payloadAsJsonObject();
        assertThat(jsonObject.get("cotrId"), is(notNullValue()));

        final JsonObject defObject = jsonObject.getJsonArray("defendantSubject").getJsonObject(0);
        assertThat(defObject.getJsonObject("localAuthorityDetailsForYouthDefendants"), is(notNullValue()));
    }

    @Test
    public void shouldSubmitCpsServeCotrInPendingStatus() {
        this.cpsServeCotrReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_COTR_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_COTR,
                "importCase/stagingprosecutors.cps-serve-cotr.json", CPS_SERVE_COTR)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        final UUID submissionId = fetchSubmissionId(urlResponse);
        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsServeCotrReceivedEventsConsumer);
        final String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_COTR_RECEIVED));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getJsonArray("defendantSubject").getJsonObject(0).getJsonObject("localAuthorityDetailsForYouthDefendants"), is(notNullValue()));
    }

    @Test
    public void shouldRaiseBadRequestWhenSubmitCpsUpdateCotr() {
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_UPDATE_COTR + "/7e2f843e-d639-40b3-8611-8015f3a18958",
                "importCase/stagingprosecutors.cps-update-cotr-bad-request.json", CPS_UPDATE_COTR)) {
            assertThat(BAD_REQUEST.getStatusCode(), is(response.getStatus()));
        }
    }

    @Test
    public void shouldUpdateSubmissionStatusBasedOnPublicEvent() {
        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);

        final JsonObject eventPayload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("submissionStatus", "SUCCESS")
                .build();

        sendPublicEventToUpdateSubmissionStatus("public.prosecutioncasefile.cps-serve-material-status-updated", eventPayload, submissionId);
        final Submission submission = pollForSubmission(submissionId, SUCCESS);
        assertThat(submission.getCompletedAt(), notNullValue());
    }

    @Test
    public void shouldUpdateSubmissionStatus_SubmissionWithWarnings_BasedOnPublicEvent() {
        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);

        List<Problem> warnings = singletonList(Problem.problem()
                .withCode("INVALID_DEFENDANTS_PROVIDED")
                .withValues(singletonList(ProblemValue.problemValue()
                        .withKey("asn")
                        .withValue("invalidAsn")
                        .build())).build());
        final JsonObject eventPayload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("submissionStatus", "SUCCESS_WITH_WARNINGS")
                .add("warnings", transformErrorsToJsonArray(warnings))
                .build();

        sendPublicEventToUpdateSubmissionStatus("public.prosecutioncasefile.cps-serve-material-status-updated", eventPayload, submissionId);
        final Submission submission = pollForSubmission(submissionId, SUCCESS_WITH_WARNINGS);
        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getWarnings(), notNullValue());
        assertThat(submission.getWarnings(), hasSize(1));
        assertThat(submission.getWarnings().get(0).code, is("INVALID_DEFENDANTS_PROVIDED"));
        assertThat(submission.getWarnings().get(0).values, hasSize(1));
        assertThat(submission.getWarnings().get(0).values.get(0).key, is("asn"));
        assertThat(submission.getWarnings().get(0).values.get(0).value, is("invalidAsn"));
    }


    @Test
    public void shouldUpdateSubmissionStatus_Rejected_BasedOnPublicEvent() {
        final String urlResponse;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }

        UUID submissionId = fetchSubmissionId(urlResponse);

        List<Problem> errors = singletonList(Problem.problem()
                .withCode("INVALID_DEFENDANTS_PROVIDED")
                .withValues(singletonList(ProblemValue.problemValue()
                        .withKey("asn")
                        .withValue("invalidAsn")
                        .build())).build());
        final JsonObject eventPayload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("submissionStatus", "REJECTED")
                .add("errors", transformErrorsToJsonArray(errors))
                .add("warnings", createArrayBuilder().build())
                .build();

        sendPublicEventToUpdateSubmissionStatus("public.prosecutioncasefile.cps-serve-material-status-updated", eventPayload, submissionId);
        final Submission submission = pollForSubmission(submissionId, REJECTED);
        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getErrors(), notNullValue());
        assertThat(submission.getErrors(), hasSize(1));
        assertThat(submission.getErrors().get(0).code, is("INVALID_DEFENDANTS_PROVIDED"));
        assertThat(submission.getErrors().get(0).values, hasSize(1));
        assertThat(submission.getErrors().get(0).values.get(0).key, is("asn"));
        assertThat(submission.getErrors().get(0).values.get(0).value, is("invalidAsn"));
    }

    @Test
    public void shouldSubmitCpsServeBcmInPendingStatusAndThenRejectIt() {
        this.cpsServeBcmReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_BCM_RECEIVED).getMessageConsumerClient();

        UUID submissionId;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_BCM,
                "importCase/stagingprosecutors.cps-serve-bcm.json", CPS_SERVE_BCM)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_FORM_OPERATION_FAILED, "importCase/public.progression.form-operation-failed.json");
        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> REJECTED.toString().equals(s.getSubmissionStatus()));
    }

    @Test
    public void shouldSubmitCpsServeCotrInPendingStatusAndThenRejectIt() {
        this.cpsServeCotrReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_COTR_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_COTR,
                "importCase/stagingprosecutors.cps-serve-cotr.json", CPS_SERVE_COTR)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_COTR_OPERATION_FAILED, "importCase/public.progression.cotr-operation-failed.json");
        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> REJECTED.toString().equals(s.getSubmissionStatus()));
    }

    @Test
    public void shouldSubmitCpsServePetInPendingStatusAndThenRejectIt() {
        this.cpsServePetReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PET_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_FORM_OPERATION_FAILED, "importCase/public.progression.form-operation-failed.json");
        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> REJECTED.toString().equals(s.getSubmissionStatus()));
    }

    @Test
    public void shouldSubmitCpsServePtphInPendingStatusAndThenRejectIt() {
        this.cpsServePtphReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PTPH_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PTPH,
                "importCase/stagingprosecutors.cps-serve-ptph.json", CPS_SERVE_PTPH)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_FORM_OPERATION_FAILED, "importCase/public.progression.form-operation-failed.json");
        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> REJECTED.toString().equals(s.getSubmissionStatus()));
    }

    @Test
    public void shouldSubmitCpsServePetInPendingStatusAndThenSuccessAfterPetFormCreation() {
        this.cpsServePetReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PET_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet.json", CPS_SERVE_PET)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_PET_FORM_CREATED, "importCase/public.progression.pet-form-created.json");
        pollForSubmission(submissionId, SUCCESS);
    }

    @Test
    public void shouldSubmitCpsServeCotrInPendingStatusAndThenSuccessAfterCotrCreation() {
        this.cpsServeCotrReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_COTR_RECEIVED).getMessageConsumerClient();

        this.cpsUpdateCotrReviewNotesEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_REVIEW_NOTES_UPDATED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_COTR,
                "importCase/stagingprosecutors.cps-serve-cotr.json", CPS_SERVE_COTR)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_COTR_CREATED, "importCase/public.progression.cotr-created.json");
        pollForSubmission(submissionId, SUCCESS);
    }

    @Test
    public void shouldSubmitCpsServeBcmInPendingStatusAndThenSuccessAfterFormCreation() {
        this.cpsServeBcmReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_BCM_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_BCM,
                "importCase/stagingprosecutors.cps-serve-bcm.json", CPS_SERVE_BCM)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_FORM_CREATED, "importCase/public.progression.form-created.json");
        pollForSubmission(submissionId, SUCCESS);
    }

    @Test
    public void shouldSubmitCpsServePtphInPendingStatusAndThenSuccessAfterFormCreation() {
        this.cpsServePtphReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PTPH_RECEIVED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PTPH,
                "importCase/stagingprosecutors.cps-serve-ptph.json", CPS_SERVE_PTPH)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_FORM_CREATED, "importCase/public.progression.form-created.json");
        pollForSubmission(submissionId, SUCCESS);
    }

    @Test
    public void shouldServECotrThenCotrCreatedThenUpdateCotrReviewNotesWithSuccess() {

        this.cpsUpdateCotrReviewNotesEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_REVIEW_NOTES_UPDATED).getMessageConsumerClient();

        UUID submissionId;

        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_COTR,
                "importCase/stagingprosecutors.cps-serve-cotr.json", CPS_SERVE_COTR)) {
            final String urlResponse = response.readEntity(String.class);
            assertThat(ACCEPTED.getStatusCode(), is(response.getStatus()));
            submissionId = fetchSubmissionId(urlResponse);
            pollForSubmission(submissionId, SubmissionStatus.PENDING);
        }

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_COTR_CREATED, "importCase/public.progression.cotr-created.json");
        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> SUCCESS.toString().equals(s.getSubmissionStatus()));

        publishPublicProgressionSubmissionAndWaitForProcessing(submissionId,
                EVENT_COTR_REVIEW_NOTES_UPDATED, "importCase/public.progression.cotr-review-notes-updated.json");

        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageAsString(this.cpsUpdateCotrReviewNotesEventsConsumer);
        final String eventName = jsonEnvelope.get().metadata().name();
        assertThat(eventName, is(PUBLIC_EVENT_REVIEW_NOTES_UPDATED));

        poller().setPathParameter("submissionId", submissionId.toString())
                .pollUntil(s -> SUCCESS.toString().equals(s.getSubmissionStatus()));
    }

    private void verifyPetReceivedPublicEvent(final CpsPetReceivedDetails cpsPetReceivedDetails, final UUID submissionId) {
        assertThat(cpsPetReceivedDetails, notNullValue());
        assertThat(cpsPetReceivedDetails.getCpsDefendantOffences(), notNullValue());
        List<CpsDefendantOffences> cpsDefendantOffencesList = cpsPetReceivedDetails.getCpsDefendantOffences();
        assertThat(cpsDefendantOffencesList, hasSize(1));
        cpsDefendantOffencesList.forEach(this::verifyCpsDefendantOffenceObject);

        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getProsecutingAuthority(), is(PROSECUTION_AUTHORITY));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getUrn(), is(URN));
        assertThat(cpsPetReceivedDetails.getSubmissionId(), is(submissionId));
        assertThat(cpsPetReceivedDetails.getSubmissionStatus().toString(), is(PENDING.toString()));

        assertThat(cpsPetReceivedDetails.getPetFormData(), notNullValue());
        assertThat(cpsPetReceivedDetails.getPetFormData().getDefence(), is(notNullValue()));
        final Defence defence = cpsPetReceivedDetails.getPetFormData().getDefence();
        assertThat(defence.getDefendants(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsDefendantId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(defence.getDefendants().get(0).getId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getCpsOffences(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getWording(), is(OFFENCE_WORDING));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getDate(), is(notNullValue()));

        assertThat(cpsPetReceivedDetails.getPetFormData().getProsecution(), is(notNullValue()));
        final Prosecution prosecution = cpsPetReceivedDetails.getPetFormData().getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> verifyProsecutorWitnessObject(Y, witness));

        assertThat(prosecution.getDynamicFormAnswers(), is(notNullValue()));
        assertThat(prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup(), is(notNullValue()));
        final ApplicationsForDirectionsGroup applicationsForDirectionsGroup = prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup();
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutor(), is(Y));
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutorYesGroup().getVariationStandardDirectionsProsecutorYesGroupDetails(), is(DIRECTION_DETAILS));
        assertThat(applicationsForDirectionsGroup.getGroundRulesQuestioning(), is(Y));

        assertThat(prosecution.getDynamicFormAnswers().getProsecutorGroup(), is(notNullValue()));
        verifyProsecutorGroupObject(N, Y, prosecution);

        assertThat(cpsPetReceivedDetails.getReviewingLawyer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getPhone(), is(PHONE));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getPhone(), is(PHONE));

        assertThat(defence.getDefendants().get(0).getAssociatedPerson(), is(notNullValue()));
        AssociatedPerson associatedPerson = defence.getDefendants().get(0).getAssociatedPerson();
        assertThat(associatedPerson.getAuthorityDetails(), notNullValue());
        assertThat(associatedPerson.getGuardianDetails(), notNullValue());

        assertThat(cpsPetReceivedDetails.getIsYouth(), notNullValue());
    }

    private void verifyBcmReceivedPublicEvent(final CpsBcmReceivedDetails cpsBcmReceivedDetails, final UUID submissionId) {
        assertThat(cpsBcmReceivedDetails, notNullValue());
        assertThat(cpsBcmReceivedDetails.getCpsDefendantOffences(), notNullValue());
        List<CpsDefendantOffences> cpsDefendantOffencesList = cpsBcmReceivedDetails.getCpsDefendantOffences();
        assertThat(cpsDefendantOffencesList, hasSize(1));
        cpsDefendantOffencesList.forEach(this::verifyCpsDefendantOffenceObject);

        assertThat(cpsBcmReceivedDetails.getTag(), CoreMatchers.notNullValue());
        assertThat(cpsBcmReceivedDetails.getEvidencePrePTPH(), CoreMatchers.notNullValue());
        assertThat(cpsBcmReceivedDetails.getProsecutionCaseSubject(), CoreMatchers.notNullValue());
        assertThat(cpsBcmReceivedDetails.getProsecutionCaseSubject(), CoreMatchers.notNullValue());
        assertThat(cpsBcmReceivedDetails.getProsecutionCaseSubject(), notNullValue());
        assertThat(cpsBcmReceivedDetails.getProsecutionCaseSubject().getProsecutingAuthority(), is(PROSECUTION_AUTHORITY));
        assertThat(cpsBcmReceivedDetails.getProsecutionCaseSubject().getUrn(), is(URN));
        assertThat(cpsBcmReceivedDetails.getSubmissionId(), is(submissionId));
        assertThat(cpsBcmReceivedDetails.getSubmissionStatus().toString(), is(PENDING.toString()));

    }

    private void verifyProsecutorGroupObject(final String no, final String yes, final Prosecution prosecution) {
        final ProsecutorGroup prosecutorGroup = prosecution.getDynamicFormAnswers().getProsecutorGroup();
        assertThat(prosecutorGroup.getProsecutorServeEvidence(), is(no));

        assertThat(prosecutorGroup.getProsecutionCompliance(), is(no));
        assertThat(prosecutorGroup.getProsecutionComplianceNoGroup().getProsecutionComplianceDetailsNo(), is(NO_COMPLIANCE_DETAILS));

        assertThat(prosecutorGroup.getPendingLinesOfEnquiry(), is(no));
        assertThat(prosecutorGroup.getPendingLinesOfEnquiryYesGroup().getPendingLinesOfEnquiryYesGroup(), is(DETAILS));

        assertThat(prosecutorGroup.getSlaveryOrExploitation(), is(no));
        assertThat(prosecutorGroup.getSlaveryOrExploitationYesGroup().getSlaveryOrExploitationDetails(), is(SLAVERY_DETAILS));

        assertThat(prosecutorGroup.getRelyOn().size(), is(12));
        assertThat(prosecutorGroup.getRelyOn().get(0).toString(), is("admissions"));

        assertThat(prosecutorGroup.getDisplayEquipment(), is(yes));
        assertThat(prosecutorGroup.getDisplayEquipmentYesGroup().getDisplayEquipmentDetails(), is(EQUIPMENT_DETAILS));

        assertThat(prosecutorGroup.getPointOfLaw(), is(yes));
        assertThat(prosecutorGroup.getPointOfLawYesGroup().getPointOfLawDetails(), is(LAW_DETAILS));

        assertThat(prosecution.getDynamicFormAnswers().getAdditionalInformation(), notNullValue());
    }

    private void verifyProsecutorWitnessObject(final String yes, final cpp.moj.gov.uk.staging.prosecutors.json.schemas.Witnesses witness) {
        assertThat(witness.getAge(), is(14));
        assertThat(witness.getFirstName(), is(FORE_NAME));
        assertThat(witness.getInterpreterRequired(), is(Y));
        assertThat(witness.getLanguageAndDialect(), is(WELSH));
        assertThat(witness.getLastName(), is(LAST_NAME));
        assertThat(witness.getMeasuresRequired(), is(singletonList(MEASURE_DETAILS)));
        assertThat(witness.getProsecutionProposesWitnessAttendInPerson(), is(Y));
        assertThat(witness.getSpecialOtherMeasuresRequired(), is(yes));
    }

    private void verifyCpsDefendantOffenceObject(final CpsDefendantOffences cpsDefendantOffence) {
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is("test"));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(), is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(), is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(), is(OFFENCE_WORDING));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(), is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getLocalAuthorityDetailsForYouthDefendants(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getParentGuardianForYouthDefendants(), is(notNullValue()));
    }

    private JsonArray transformErrorsToJsonArray(final Collection<Problem> errors) {
        ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        if (errors == null) {
            return null;
        }

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        errors.stream()
                .map(objectToJsonObjectConverter::convert)
                .forEach(arrayBuilder::add);

        return arrayBuilder.build();
    }

    @Test
    public void shouldNotSubmitCpsServePetInPendingStatus() {
        this.cpsServePetReceivedEventsConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_PET_RECEIVED).getMessageConsumerClient();

        final String urlResponse;
        try (Response response = importServeMaterialCaseAndWaitUntilReady(WRITE_BASE_URI_PET,
                "importCase/stagingprosecutors.cps-serve-pet-schema-error.json", CPS_SERVE_PET)) {
            assertThat(BAD_REQUEST.getStatusCode(), is(response.getStatus()));
            urlResponse = response.readEntity(String.class);
        }
        final JsonObject jsonObject = stringToJsonObject(urlResponse);
        assertThat(jsonObject.get("validationErrors").toString(), containsString("prosecutionCaseSubject/prosecutingAuthority: expected minLength: 1, actual: 0"));
    }

    private JsonObject stringToJsonObject(String response) {
        try (StringReader reader = new StringReader(response)) {
            return JsonObjects.createReader(reader).readObject();
        }
    }
}
