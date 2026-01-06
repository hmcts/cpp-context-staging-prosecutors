package uk.gov.moj.cpp.staging.prosecutorapi.utils;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.apache.http.entity.mime.MultipartEntityBuilder.create;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.ResourcesUtils.asJsonObject;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutorapi.model.query.Submission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.ReadContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.json.JSONObject;

public class StagingProsecutors {

    public static final String OUCODE = "B01BH00";
    public static final String OUCODE_LOWER_CASE = "b01bh00";
    public static final String DIFFERENT_OUCODE = "B01BH01";
    public static final String SJP_CONTENT_TYPE = "application/vnd.hmcts.cjs.sjp-prosecution+json";
    public static final String SJP_CONTENT_TYPE_V2 = "application/vnd.hmcts.cjs.sjp-prosecution.v2+json";
    public static final String SUMMONS_CONTENT_TYPE_V2 = "application/vnd.hmcts.cjs.summons-prosecution.v2+json";
    public static final String CHARGE_CONTENT_TYPE_V2 = "application/vnd.hmcts.cjs.charge-prosecution.v2+json";
    public static final String REQUISITION_CONTENT_TYPE_V2 = "application/vnd.hmcts.cjs.requisition-prosecution.v2+json";
    private static final RestClient restClient = new RestClient();
    private static final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
    private static final String WRITE_BASE_URI_V2 = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v2";

    private static final String WRITE_BASE_URI = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1";
    private static final String READ_BASE_URI = getBaseUri()
            + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v1";
    private static final String READ_BASE_URI_V2 = getBaseUri()
            + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v2";

    private static final String READ_BASE_CPS_URI_V1 = getBaseUri()
            + "/stagingprosecutors-query-api/query/api/rest/stagingprosecutors/v1";
    public static final String WRITE_BASE_URI_PET = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/servePet";
    public static final String WRITE_BASE_URI_BCM = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/serveBcm";
    public static final String WRITE_BASE_URI_PTPH = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/servePtph";
    public static final String WRITE_BASE_URI_COTR = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/serveCotr";
    public static final String WRITE_BASE_URI_UPDATE_COTR = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/updateCotr";

    private static final String WRITE_BASE_URI_V3 = getBaseUri()
            + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v3";

    private static final String PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL = WRITE_BASE_URI + "/prosecutions/%s/materials";
    private static final String PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL_V2 = WRITE_BASE_URI_V2 + "/prosecutions/%s/materials";
    private static final String PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL_V3 = WRITE_BASE_URI_V3 + "/prosecutions/materials";
    public static final String CONTEXT_NAME = "stagingprosecutors";
    private static final String EVENT_MATERIAL_SUBMISSION_SUCCESSFUL = "stagingprosecutors.event.material-submission-successful";
    private static final String EVENT_MATERIAL_SUBMISSION_REJECTED = "stagingprosecutors.event.material-submission-rejected";
    private static final String EVENT_MATERIAL_SUBMISSION_PENDING_WITH_WARNINGS = "stagingprosecutors.event.submission-pending-with-warnings";
    private static final String EVENT_SUBMISSION_SUCCESSFUL_WITH_WARNINGS = "stagingprosecutors.event.submission-successful-with-warnings";
    private static final String PUBLIC_EVENT_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED = "public.prosecutioncasefile.material-rejected";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_V2 = "public.prosecutioncasefile.material-rejected-v2";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_WITH_WARNINGS = "public.prosecutioncasefile.material-rejected-with-warnings";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_SUBMISSION_PENDING_WITH_WARNINGS = "public.prosecutioncasefile.material-pending-with-warnings";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    private static final String PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED = "public.progression.court-document-added";
    private static final String PROSECUTOR_DOCUMENT_CPS_UPLOAD_COMMAND_URL_V1 = WRITE_BASE_URI + "/prosecutions/cps/%s/materials";

    public static final String CPS_SERVE_PET = "application/vnd.stagingprosecutors.cps-serve-pet+json";
    public static final String CPS_SERVE_BCM = "application/vnd.stagingprosecutors.cps-serve-bcm+json";
    public static final String CPS_SERVE_PTPH = "application/vnd.stagingprosecutors.cps-serve-ptph+json";
    public static final String CPS_SERVE_COTR = "application/vnd.stagingprosecutors.cps-serve-cotr+json";
    public static final String CPS_UPDATE_COTR = "application/vnd.stagingprosecutors.cps-update-cotr+json";


    public static Submission pollForSubmission(final UUID submissionId, final Matcher... matchers) {
        return getSubmission(submissionId, allOf(matchers));
    }

    public static Submission pollForSubmission(final UUID submissionId, final SubmissionStatus expectedSubmissionStatus) {
        return getSubmission(submissionId, withJsonPath("status", is(expectedSubmissionStatus.toString())));
    }

    public static Submission getSubmission(final UUID submissionId, final Matcher<? super ReadContext> matcher) {
        final String payload = poll(getRequestParams(submissionId))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, Submission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static SubmissionStatus getSubmissionStatusForCPS(final UUID submissionId) {
        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.cps.v1.Submission submission = getSubmissionForCpsV1(submissionId);
        return SubmissionStatus.valueOf(submission.getSubmissionStatus());
    }


    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission pollForSubmissionV2(final UUID submissionId, final String oucode, final Matcher... matchers) {
        return getSubmissionV2(submissionId, allOf(matchers), oucode);
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission pollForSubmissionV2(final UUID submissionId, final SubmissionStatus expectedSubmissionStatus, final String oucode) {
        return getSubmissionV2(submissionId, withJsonPath("status", is(expectedSubmissionStatus.toString())), oucode);
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission pollForSubmissionV3(final UUID submissionId, final SubmissionStatus expectedSubmissionStatus, final String oucode) {
        return getSubmissionV3(submissionId, withJsonPath("status", is(expectedSubmissionStatus.toString())), oucode);
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.SjpSubmission pollForSubmissionSjpV3(final UUID submissionId, final SubmissionStatus expectedSubmissionStatus, final String oucode) {
        return getSubmissionSJPV3(submissionId, withJsonPath("status", is(expectedSubmissionStatus.toString())), oucode);
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission getSubmissionV2(final UUID submissionId, final String oucode) {
        return getSubmissionV2(submissionId, anything(), oucode);
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.cps.v1.Submission getSubmissionForCpsV1(final UUID submissionId) {
        return getSubmissionForCpsV1(submissionId, anything());
    }


    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission getSubmissionV2(final UUID submissionId, final Matcher<? super ReadContext> matcher, final String oucode) {
        final String payload = poll(getRequestParamsV2(submissionId, oucode))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission getSubmissionV3(final UUID submissionId, final Matcher<? super ReadContext> matcher, final String oucode) {
        final String payload = poll(getRequestParamsV3(submissionId, oucode))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.SjpSubmission getSubmissionSJPV3(final UUID submissionId, final Matcher<? super ReadContext> matcher, final String oucode) {
        final String payload = poll(getRequestParamsV3ForSjp(submissionId, oucode))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.SjpSubmission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static uk.gov.moj.cpp.staging.prosecutorapi.model.query.cps.v1.Submission getSubmissionForCpsV1(final UUID submissionId, final Matcher<? super ReadContext> matcher) {
        final String payload = poll(getRequestParams4CpsV1(submissionId))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(matcher)
                )
                .getPayload();

        try {
            return mapper.readValue(payload, uk.gov.moj.cpp.staging.prosecutorapi.model.query.cps.v1.Submission.class);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void verifyForbiddenHttpResponseCodeForV3Submission(final UUID submissionId, final String ouCode) {
        final Response.Status status = poll(getRequestParamsV3(submissionId, ouCode))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(FORBIDDEN)
                )
                .getStatus();
        assertThat(status, is(FORBIDDEN));
    }


    public static void verifyForbiddenHttpResponseCode(final UUID submissionId) {
        final Response.Status status = poll(getRequestParams4CpsV1(submissionId))
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(20, SECONDS)
                .until(
                        status().is(FORBIDDEN)
                )
                .getStatus();
        assertThat(status, is(FORBIDDEN));
    }

    private static RequestParams getRequestParams(final UUID submissionId) {
        final String url = READ_BASE_URI + "/submissions/" + submissionId;
        final String mediaType = "application/vnd.hmcts.cjs.submission+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }

    private static RequestParams getRequestParamsV2(final UUID submissionId, final String oucode) {
        final String url = READ_BASE_URI_V2 + "/submissions/" + submissionId + "/" + oucode;
        final String mediaType = "application/vnd.hmcts.cjs.submission.v2+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }

    private static RequestParams getRequestParamsV3(final UUID submissionId, final String oucode) {
        final String url = READ_BASE_URI_V2 + "/submissions/" + submissionId + "/" + oucode;
        final String mediaType = "application/vnd.hmcts.cjs.submission.v3+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }

    private static RequestParams getRequestParamsV3ForSjp(final UUID submissionId, final String oucode) {
        final String url = READ_BASE_URI_V2 + "/submissions/" + submissionId + "/" + oucode;
        final String mediaType = "application/vnd.hmcts.cjs.sjp.submission.v3+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }


    private static RequestParams getRequestParams4CpsV1(final UUID submissionId) {
        final String url = READ_BASE_CPS_URI_V1 + "/submissions/cps/" + submissionId;
        final String mediaType = "application/vnd.hmcts.cps.submission.v1+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, UUID.randomUUID())
                .build();
    }

    public static UUID importCaseAndWaitUntilReady(final String inputFileName) {
        final String urlResponse = importCase(inputFileName);
        final UUID submissionId = extractSubmissionId(of(urlResponse));
        pollForSubmission(submissionId, PENDING);
        return submissionId;
    }

    public static UUID importCaseAndWaitUntilReadyV2SJP(final String inputFileName, final String contentType, final String ouCodeInPath, final String ouCodeInPayload) {
        final String urlResponse = importCaseV2(inputFileName, contentType, ouCodeInPath, ouCodeInPayload);
        final UUID submissionId = extractSubmissionId(of(urlResponse));
        pollForSubmissionV2(extractSubmissionId(of(urlResponse)), PENDING, ouCodeInPath);
        return submissionId;
    }

    public static UUID importCaseAndWaitUntilReadyV2(final String inputFileName, final String contentType, final String ouCodeInPath, final String ouCodeInPayload) {
        return importCaseAndWaitUntilReadyV2(inputFileName, contentType, ouCodeInPath, ouCodeInPayload, "stagingprosecutors.event.prosecution-received");
    }

    public static UUID importCaseAndWaitUntilReadyV2(final String inputFileName, final String contentType,
                                                     final String ouCodeInPath, final String ouCodeInPayload,
                                                     final String eventName) {
        final String urlResponse = importCaseV2(inputFileName, contentType, ouCodeInPath, ouCodeInPayload);
        final UUID submissionId = extractSubmissionId(of(urlResponse));
        pollForSubmission(submissionId, PENDING);
        return submissionId;
    }

    public static HttpResponse sendFileUploadRequest(final String caseUrn,
                                                     final File file,
                                                     final String materialType,
                                                     final String prosecutingAuthority) throws IOException {
        final String commandUrl = format(PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL, caseUrn);
        final HttpPost request = new HttpPost(commandUrl);

        return sendSubmitMaterialRequest(request, caseUrn, file, prosecutingAuthority, materialType);
    }

    public static HttpResponse sendFileUploadRequestV2(final String caseUrn,
                                                       final File file,
                                                       final String materialType,
                                                       final String prosecutingAuthority,
                                                       final String ouCodeInPath) throws IOException {
        final String commandUrl = format(PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL_V2 + "/" + ouCodeInPath, caseUrn);
        final HttpPost request = new HttpPost(commandUrl);

        return sendSubmitMaterialRequest(request, caseUrn, file, prosecutingAuthority, materialType);
    }

    public static HttpResponse sendFileUploadRequestV3(final File file, final String prosecutingAuthority,
                                                       final boolean flag, final UUID applicationId, final String ouCode) throws IOException {

        final HttpPost request = new HttpPost(PROSECUTOR_DOCUMENT_UPLOAD_COMMAND_URL_V3 + "/" + ouCode);
        return sendSubmitMaterialRequestv3(request, file, prosecutingAuthority, flag, applicationId);
    }

    public static HttpResponse sendCpsFileUploadRequestV1(final String caseUrn,
                                                          final File file,
                                                          final String materialType,
                                                          final String prosecutingAuthority) throws IOException {
        final String commandUrl = format(PROSECUTOR_DOCUMENT_CPS_UPLOAD_COMMAND_URL_V1, caseUrn);
        final HttpPost request = new HttpPost(commandUrl);

        return sendSubmitMaterialRequest(request, caseUrn, file, prosecutingAuthority, materialType);
    }

    private static HttpResponse sendSubmitMaterialRequest(final HttpPost request, final String caseUrn, final File file, final String prosecutingAuthority, final String materialType) throws IOException {
        final MultipartEntityBuilder multipartEntityBuilder = create()
                .setMode(BROWSER_COMPATIBLE)
                .addBinaryBody("material", file, APPLICATION_OCTET_STREAM, file.getName())
                .addTextBody("prosecutingAuthority", prosecutingAuthority)
                .addTextBody("caseUrn", caseUrn);

        if (materialType != null) {
            multipartEntityBuilder.addTextBody("materialType", materialType);
        }

        request.setEntity(multipartEntityBuilder.build());
        request.setHeader(HeaderConstants.USER_ID, randomUUID().toString());


        return HttpClients.createDefault().execute(request);
    }

    private static HttpResponse sendSubmitMaterialRequestv3(final HttpPost request, final File file,
                                                            final String prosecutingAuthority, final boolean flag,
                                                            final UUID applicationId) throws IOException {

        final MultipartEntityBuilder multipartEntityBuilder = create()
                .setMode(BROWSER_COMPATIBLE)
                .addBinaryBody("material", file, APPLICATION_OCTET_STREAM, file.getName())
                .addTextBody("materialType", "SJPN")
                .addTextBody("materialName", "defendant-material")
                .addTextBody("materialContentType", "image/jpeg")
                .addTextBody("fileName", "Material-File")
                .addTextBody("sectionOrderSequence", "1")
                .addTextBody("caseSubFolderName", "Defendant-Material")
                .addTextBody("exhibit", createExhibit().toString(), ContentType.APPLICATION_JSON)
                .addTextBody("tag", createTags().build().toString())
                .addTextBody("witnessStatement", createWitnessStatement().toString(), ContentType.APPLICATION_JSON);
        if (flag) {
            multipartEntityBuilder.addTextBody("prosecutionCaseSubject", createProsecutionCaseSubject(prosecutingAuthority).toString(), ContentType.APPLICATION_JSON);
        } else {
            multipartEntityBuilder.addTextBody("courtApplicationSubject", createCourtApplicationSubject(applicationId).toString(), ContentType.APPLICATION_JSON);
        }

        request.setEntity(multipartEntityBuilder.build());
        request.setHeader(HeaderConstants.USER_ID, randomUUID().toString());

        return HttpClients.createDefault().execute(request);
    }

    private static JsonObject createProsecutionCaseSubject(final String prosecutingAuthority) {
        return createObjectBuilder()
                .add("caseUrn", randomAlphabetic(10))
                .add("prosecutingAuthority", prosecutingAuthority)
                .add("defendantSubject", createObjectBuilder()
                        .add("prosecutorPersonDefendantDetails", createObjectBuilder()
                                .add("forename", "David")
                                .add("title", "Mr")
                                .add("surname", "Miller")
                                .add("prosecutorDefendantId", randomUUID().toString())
                                .add("dateOfBirth", "1985-02-03")
                                .build()))
                .build();
    }

    public static JsonObject createCourtApplicationSubject(final UUID applicationId) {
        return createObjectBuilder()
                .add("courtApplicationId", applicationId.toString())
                .build();
    }


    public static UUID extractSubmissionId(final Optional<String> submissionIdWrapper) {
        return submissionIdWrapper.map(JSONObject::new)
                .filter(eventPayload -> eventPayload.has("submissionId"))
                .map(eventPayload -> fromString(eventPayload.getString("submissionId")))
                .orElseThrow(() -> new AssertionError("Impossible retrieve submissionId"));
    }

    public static void publishPublicProgressionCourtDocumentAdded(final UUID submissionId) {

        sendPublicMessageAndExpectPrivateMessage(
                EVENT_MATERIAL_SUBMISSION_SUCCESSFUL,
                createObjectBuilder().build(),
                publicProgressionCourtDocumentAdded(submissionId),
                PUBLIC_EVENT_PROGRESSION_COURT_DOCUMENT_ADDED);
    }

    public static void publishPublicMaterialSubmissionRejected(final UUID caseId, final UUID submissionId, final Problem... problems) {

        final JsonArrayBuilder errorBuilder = JsonObjects.createArrayBuilder();

        Stream.of(problems).forEach(problem -> {
            final JsonArrayBuilder errorValuesBuilder = JsonObjects.createArrayBuilder();

            problem.values.forEach(value -> errorValuesBuilder.add(createObjectBuilder()
                    .add("key", value.key)
                    .add("value", value.value)));

            errorBuilder.add(createObjectBuilder()
                    .add("code", problem.code)
                    .add("values", errorValuesBuilder.build()));
        });

        final JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("errors", errorBuilder)
                .build();

        sendPublicEvent(eventPayload, publicMaterialRejectedMetadata(submissionId), PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED);
    }

    private static String importCaseV2(final String resourceName, final String contentType, final String ouCodeInPath, final String ouCodeInPayload) {
        try (Response response = postCommandV2(resourceName, contentType, ouCodeInPath, ouCodeInPayload)) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
            return response.readEntity(String.class);
        }
    }


    private static String importCase(final String resourceName) {
        try (Response response = postCommand(resourceName)) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
            return response.readEntity(String.class);
        }
    }

    public static String importCaseShouldFail(final String resourceName) {
        try (Response response = postCommand(resourceName)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            return response.readEntity(String.class);
        }
    }

    public static String importCaseShouldFailV2(final String resourceName) {
        try (Response response = postCommandV2(resourceName, SJP_CONTENT_TYPE_V2, OUCODE, OUCODE)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            return response.readEntity(String.class);
        }
    }

    public static void publishPublicProsecutionSubmissionSucceededAndWaitForProcessing(final UUID submissionId) {

        sendPublicEvent(asJsonObject("importCase/public.prosecutioncasefile.prosecution-submission-succeeded.json", "EXTERNAL_ID", submissionId.toString()),
                publicProsecutionMetadataForSuccessOrFailure(submissionId, PUBLIC_EVENT_PROSECUTION_SUBMISSION_SUCCEEDED), PUBLIC_EVENT_PROSECUTION_SUBMISSION_SUCCEEDED);
        pollForSubmission(submissionId, SUCCESS);
    }

    public static void publishPublicProsecutionSubmissionRejectedAndWaitForProcessing(final UUID submissionId) {
        final String publicEventName = "public.prosecutioncasefile.prosecution-rejected";

        sendPublicEvent(asJsonObject("importCase/public.prosecutioncasefile.prosecution-rejected.json", "EXTERNAL_ID", submissionId.toString()),
                publicProsecutionMetadataForSuccessOrFailure(submissionId, publicEventName), publicEventName);
        pollForSubmission(submissionId, REJECTED);
    }

    public static void publishPublicProgressionSubmissionAndWaitForProcessing(final UUID submissionId, final String publicEventName, final String eventResourceName) {
        sendPublicMessageAndExpectPrivateMessage(
                "stagingprosecutors.event.submission-status-updated",
                asJsonObject(eventResourceName, "SUBMISSION_ID", submissionId.toString()),
                publicProsecutionMetadataForSuccessOrFailure(submissionId, publicEventName), publicEventName);
    }

    public static void publishPublicProsecutionSubmissionSucceededWithWarningsAndWaitForProcessing(final UUID submissionId) {
        sendPublicMessageAndExpectPrivateMessage(
                EVENT_SUBMISSION_SUCCESSFUL_WITH_WARNINGS,
                asJsonObject("importCase/public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings.json", "EXTERNAL_ID", submissionId.toString()),
                publicProsecutionMetadataForSuccessOrFailure(submissionId, PUBLIC_EVENT_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS),
                PUBLIC_EVENT_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS);
    }

    private static void sendPublicMessageAndExpectPrivateMessage(final String privateEventName,
                                                                 final JsonObject publicEventPayload,
                                                                 final Metadata publicEventMetadata,
                                                                 final String publicEventName) {
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(privateEventName).getMessageConsumerClient();
        sendPublicEvent(publicEventPayload, publicEventMetadata, publicEventName);
        final Optional<String> message = messageConsumerClient.retrieveMessage();
        assertThat(format("'%s' message not found on stagingprosecutors.event topic", privateEventName),
                message.isPresent(),
                is(true));
    }

    private static void sendPublicEvent(final JsonObject publicEventPayload, final Metadata publicEventMetadata, final String publicEventName) {
        final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        final JsonEnvelope jsonEnvelope = envelopeFrom(publicEventMetadata, publicEventPayload);
        messageProducerClientPublic.sendMessage(publicEventName, jsonEnvelope);
    }

    private static Metadata publicProsecutionMetadataForSuccessOrFailure(final UUID submissionId, final String publicEventName) {
        return publicProsecutionMetadata(submissionId, publicEventName);
    }

    private static Metadata publicProgressionCourtDocumentAdded(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, "public.progression.court-document-added");
    }

    private static Metadata publicMaterialRejectedMetadata(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, "public.prosecutioncasefile.material-rejected");
    }

    private static Metadata publicMaterialRejectedV2Metadata(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_V2);
    }

    private static Metadata publicMaterialRejectedWithWarnings(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_WITH_WARNINGS);
    }

    private static Metadata publicMaterialSubmissionPendingWithWarnings(final UUID submissionId) {
        return publicProsecutionMetadata(submissionId, PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_SUBMISSION_PENDING_WITH_WARNINGS);
    }

    private static Metadata publicProsecutionMetadata(final UUID submissionId, final String name) {
        return metadataFrom(JsonObjects.createObjectBuilder(
                        metadataBuilder()
                                .withName(name)
                                .withUserId(randomUUID().toString())
                                .withId(UUID.randomUUID())
                                .createdAt(new UtcClock().now())
                                .build()
                                .asJsonObject())
                .add("submissionId", submissionId.toString()).build())
                .build();
    }

    private static Response postCommand(final String resourceName) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        return restClient.postCommand(WRITE_BASE_URI + "/prosecutions",
                SJP_CONTENT_TYPE,
                ResourcesUtils.readResource(resourceName), headers
        );
    }

    public static Response postCommandV2(final String resourceName, final String contentType, final String ouCodeInPath, final String ouCodeInPayload) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        return restClient.postCommand(WRITE_BASE_URI_V2 + "/prosecutions/" + ouCodeInPath,
                contentType,
                ResourcesUtils.readResource(resourceName).replace("PROSECUTING_AUTHORITY", ouCodeInPayload), headers
        );
    }

    public static Response pollQuerySubmissionV2(final UUID submissionId, final String ouCodeInPath) {
        final String url = READ_BASE_URI_V2 + "/submissions/" + submissionId + "/" + ouCodeInPath;
        final String mediaType = "application/vnd.hmcts.cjs.submission.v2+json";

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        return restClient.query(url,
                mediaType,
                headers
        );
    }

    public static void publishPublicMaterialSubmissionRejectedV2(final UUID caseId, final UUID submissionId, final Problem... problems) {

        final JsonObject eventPayload = getMaterialSubmissionRejectedV3Payload(caseId, submissionId, true, false, problems);

        sendPublicMessageAndExpectPrivateMessage(
                EVENT_MATERIAL_SUBMISSION_REJECTED,
                eventPayload,
                publicMaterialRejectedV2Metadata(submissionId),
                PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_V2);
    }

    public static void publishPublicMaterialSubmissionRejectedWithWarning(final UUID caseId, final UUID submissionId, final Problem... problems) {

        final JsonObject eventPayload = getMaterialSubmissionRejectedV3Payload(caseId, submissionId, true, true, problems);

        sendPublicMessageAndExpectPrivateMessage(
                EVENT_MATERIAL_SUBMISSION_REJECTED,
                eventPayload,
                publicMaterialRejectedWithWarnings(submissionId),
                PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_REJECTED_WITH_WARNINGS);
    }

    public static void publishPublicMaterialSubmissionPendingWithWarning(final UUID caseId, final UUID submissionId, final Problem... problems) {

        final JsonObject eventPayload = getMaterialSubmissionRejectedV3Payload(caseId, submissionId, false, true, problems);

        sendPublicMessageAndExpectPrivateMessage(
                EVENT_MATERIAL_SUBMISSION_PENDING_WITH_WARNINGS,
                eventPayload,
                publicMaterialSubmissionPendingWithWarnings(submissionId),
                PUBLIC_EVENT_PROSECUTIONCASEFILE_MATERIAL_SUBMISSION_PENDING_WITH_WARNINGS);
    }


    public static JsonObject createWitnessStatement() {
        return createObjectBuilder()
                .add("statementNumber", "1")
                .add("statementDate", "2021-03-09T14:30:04.881Z")
                .build();
    }

    public static JsonObject createExhibit() {
        return createObjectBuilder()
                .add("reference", "material-reference")
                .build();
    }

    public static JsonArrayBuilder createTags() {
        JsonObject tag1 = createObjectBuilder()
                .add("name", "material-tag")
                .add("isSpltMergeTag", true)
                .build();
        JsonObject tag2 = createObjectBuilder()
                .add("code", "6")
                .build();
        return JsonObjects.createArrayBuilder()
                .add(tag1)
                .add(tag2);
    }

    private static JsonObject getMaterialSubmissionRejectedV3Payload(final UUID caseId, final UUID submissionId, final boolean errorFlag, final boolean warningFlag, final Problem... problems) {

        final JsonArrayBuilder errorBuilder = JsonObjects.createArrayBuilder();
        Stream.of(problems).forEach(problem -> {
            final JsonArrayBuilder errorValuesBuilder = JsonObjects.createArrayBuilder();

            problem.values.forEach(value -> errorValuesBuilder.add(createObjectBuilder()
                    .add("key", value.key)
                    .add("value", value.value)));

            errorBuilder.add(createObjectBuilder()
                    .add("code", problem.code)
                    .add("values", errorValuesBuilder.build()));
        });

        final JsonObjectBuilder builder = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("caseId", caseId.toString())
                .add("material", randomUUID().toString())
                .add("materialType", "SJPN")
                .add("materialName", "defendant-material")
                .add("materialContentType", "image/jpeg")
                .add("fileName", "Material-File")
                .add("sectionOrderSequence", 1)
                .add("caseSubFolderName", "Defendant-Material")
                .add("prosecutionCaseSubject", createObjectBuilder()
                        .add("caseUrn", randomAlphabetic(10))
                        .add("ouCode", OUCODE)
                        .add("prosecutingAuthority", randomAlphabetic(7))
                        .add("defendantSubject", createObjectBuilder()
                                .add("prosecutorPersonDefendantDetails", createObjectBuilder()
                                        .add("forename", randomAlphabetic(10))
                                        .add("surname", randomAlphabetic(10))
                                        .add("prosecutorDefendantId", randomUUID().toString())
                                        .add("dateOfBirth", "1985-02-03")
                                        .add("title", "Mr")
                                        .build())
                                .build())
                        .build());
        if (errorFlag && warningFlag) {
            builder.add("errors", createError(problems));
            builder.add("warnings", createError(problems));
        } else if (errorFlag) {
            builder.add("errors", createError(problems));
        } else if (warningFlag) {
            builder.add("warnings", createError(problems));
        }


        return builder.build();
    }

    private static JsonArrayBuilder createError(final Problem... problems) {
        final JsonArrayBuilder errorBuilder = JsonObjects.createArrayBuilder();
        Stream.of(problems).forEach(problem -> {
            final JsonArrayBuilder errorValuesBuilder = JsonObjects.createArrayBuilder();

            problem.values.forEach(value -> errorValuesBuilder.add(createObjectBuilder()
                    .add("key", value.key)
                    .add("value", value.value)));

            errorBuilder.add(createObjectBuilder()
                    .add("code", problem.code)
                    .add("values", errorValuesBuilder.build()));
        });
        return errorBuilder;
    }

    public static Response importServeMaterialCaseAndWaitUntilReady(final String url, final String inputFileName, final String contentType) {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());
        return restClient.postCommand(url, contentType, ResourcesUtils.readResource(inputFileName), headers);
    }

    public static UUID fetchSubmissionId(final String urlResponse) {
        return extractSubmissionId(of(urlResponse));

    }

    public static void sendPublicEventToUpdateSubmissionStatus(final String publicEventName, final JsonObject publicEventPayload, final UUID submissionId) {
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("stagingprosecutors.event.submission-status-updated").getMessageConsumerClient();
        Metadata publicEventMetadata = metadataFrom(JsonObjects.createObjectBuilder(
                        metadataBuilder()
                                .withName(publicEventName)
                                .withUserId(randomUUID().toString())
                                .withId(UUID.randomUUID())
                                .createdAt(new UtcClock().now())
                                .build()
                                .asJsonObject())
                .add("submissionId", submissionId.toString()).build())
                .build();

        sendPublicEvent(publicEventPayload, publicEventMetadata, publicEventName);

        final Optional<String> message = messageConsumerClient.retrieveMessage();

        MatcherAssert.assertThat(format("'%s' message not found on stagingprosecutors.event topic", "stagingprosecutors.event.submission-status-updated"),
                message.isPresent(),
                CoreMatchers.is(true));
    }
}
