package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.SubmissionService;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionQueryViewTest {
    @InjectMocks
    private SubmissionQueryView view;

    @Mock
    private SubmissionService service;

    @Mock
    private ReferenceDataService referenceDataService;

    final UUID submissionId = UUID.randomUUID();
    final UUID applicationId = UUID.randomUUID();
    final SubmissionType submissionType = randomEnum(SubmissionType.class).next();

    final JsonEnvelope envelope = envelopeFrom(
            metadataWithRandomUUID("stagingprosecutors.query.submission"),
            createObjectBuilder()
                    .add("submissionId", submissionId.toString())
                    .build()
    );

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForQuerySubmission() {
        final String oucode1 = "oucode";
        final String oucode2 = "oucode2";

        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                oucode2,
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", oucode1)
                .build();
        assertThrows(ForbiddenException.class, () -> view.querySubmissionV2(payload));
    }

    @Test
    public void queriesForSubmissionV2UsingRepositoryReturnsNullPayloadWhenNotFound() {
        when(service.getSubmission(submissionId)).thenReturn(Optional.empty());

        final JsonObject jsonObject = view.querySubmissionV2(envelope.payloadAsJsonObject());

        assertNull(jsonObject);
    }

    @Test
    public void queriesForSjpSubmissionV2UsingRepositoryReturnsNullPayloadWhenNotFound() {
        when(service.getSubmission(submissionId)).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = view.querySubmissionSjpV2(envelope);

        assertThat(jsonEnvelope.payload(), is(JsonValue.NULL));
    }

    @Test
    public void queriesForSubmissionV2UsingRepository() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCode",
                createArrayBuilder().add("error").build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);

        final JsonArray caseErrors = createArrayBuilder().add("caseErrors").build();
        final JsonArray caseWarnings = createArrayBuilder().add("caseWarnings").build();
        final JsonArray defendantErrors = createArrayBuilder().add("defendantErrors").build();
        final JsonArray defendantWarnings = createArrayBuilder().add("defendantWarnings").build();
        submission.setCaseErrors(caseErrors);
        submission.setCaseWarnings(caseWarnings);
        submission.setDefendantErrors(defendantErrors);
        submission.setDefendantWarnings(defendantWarnings);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", submission.getOuCode())
                .build();

        final JsonObject jsonObject = view.querySubmissionV2(payload);

        assertThat(jsonObject.getString("id"), is(submissionId.toString()));
        assertThat(jsonObject.getString("status"), is("PENDING"));
        assertThat(jsonObject.getString("type"), is(submissionType.toString()));
        assertThat(jsonObject.getJsonArray("caseErrors"), is(caseErrors));
        assertThat(jsonObject.getJsonArray("caseWarnings"), is(caseWarnings));
        assertThat(jsonObject.getJsonArray("defendantErrors"), is(defendantErrors));
        assertThat(jsonObject.getJsonArray("defendantWarnings"), is(defendantWarnings));
        assertThat(jsonObject.getString("receivedAt"), is(receivedAt.toString()));
        assertThat(jsonObject.getString("completedAt"), is(completedAt.toString()));
    }

    @Test
    public void queriesForSubmissionV2UsingRepositoryForApplication() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                null,
                createArrayBuilder().add("error").build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                applicationId);
        submission.setCompletedAt(completedAt);

        final JsonArray caseErrors = createArrayBuilder().add("caseErrors").build();
        final JsonArray caseWarnings = createArrayBuilder().add("caseWarnings").build();
        final JsonArray defendantErrors = createArrayBuilder().add("defendantErrors").build();
        final JsonArray defendantWarnings = createArrayBuilder().add("defendantWarnings").build();
        submission.setCaseErrors(caseErrors);
        submission.setCaseWarnings(caseWarnings);
        submission.setDefendantErrors(defendantErrors);
        submission.setDefendantWarnings(defendantWarnings);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();

        final JsonObject jsonEnvelope = view.querySubmissionV2(payload);

        assertThat(jsonEnvelope.getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.getJsonArray("caseErrors"), is(caseErrors));
        assertThat(jsonEnvelope.getJsonArray("caseWarnings"), is(caseWarnings));
        assertThat(jsonEnvelope.getJsonArray("defendantErrors"), is(defendantErrors));
        assertThat(jsonEnvelope.getJsonArray("defendantWarnings"), is(defendantWarnings));
        assertThat(jsonEnvelope.getString("receivedAt"), is(receivedAt.toString()));
        assertThat(jsonEnvelope.getString("completedAt"), is(completedAt.toString()));
    }

    @Test
    public void cpsQueriesForSubmissionV1UsingRepository() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCode",
                createArrayBuilder().add("error").build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                true,
                null);
        submission.setCompletedAt(completedAt);

        final JsonArray caseErrors = createArrayBuilder().add("caseErrors").build();
        final JsonArray caseWarnings = createArrayBuilder().add("caseWarnings").build();
        final JsonArray defendantErrors = createArrayBuilder().add("defendantErrors").build();
        final JsonArray defendantWarnings = createArrayBuilder().add("defendantWarnings").build();
        submission.setCaseErrors(caseErrors);
        submission.setCaseWarnings(caseWarnings);
        submission.setDefendantErrors(defendantErrors);
        submission.setDefendantWarnings(defendantWarnings);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cps.query.submission.v1", payload);

        final JsonEnvelope jsonEnvelope = view.cpsQuerySubmissionV1(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("caseErrors"), is(caseErrors));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("caseWarnings"), is(caseWarnings));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("defendantErrors"), is(defendantErrors));
        assertThat(jsonEnvelope.payloadAsJsonObject().getJsonArray("defendantWarnings"), is(defendantWarnings));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.toString()));
    }


    @Test
    public void shouldThrowForbiddenExceptionWhenNonCpsCaseQueriedWithCpsQuerySubmission() {

        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "oucode",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cps.query.submission.v1", payload);

        assertThrows(ForbiddenException.class, () -> view.cpsQuerySubmissionV1(requestEnvelope));

    }


    @Test
    public void queriesForSubmissionUsingRepository() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCode",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);
        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonEnvelope jsonEnvelope = view.querySubmission(envelope);

        assertThat(jsonEnvelope.metadata().name(), is("hmcts.cjs.query.submission"));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.toString()));
    }

    @Test
    public void queriesForSubmissionUsingRepositoryWhenCompletedIsNull() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = null;
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCode",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);
        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonEnvelope jsonEnvelope = view.querySubmission(envelope);

        assertThat(jsonEnvelope.metadata().name(), is("hmcts.cjs.query.submission"));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.toString()));
        assertNull(jsonEnvelope.payloadAsJsonObject().get("completedAt"));
    }

    @Test
    public void queriesForSubmissionWhenDefendantUnder18ReturnsSuccessWithWarnings() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = null;
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                "ouCode",
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);
        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonEnvelope jsonEnvelope = view.querySubmission(envelope);

        assertThat(jsonEnvelope.metadata().name(), is("hmcts.cjs.query.submission"));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.toString()));
        assertNull(jsonEnvelope.payloadAsJsonObject().get("completedAt"));
    }

    @Test
    public void queriesForSjpSubmissionWhenDefendantUnder18ReturnsSuccessWithWarnings() {
        final String ouCode = "ouCode";
        final JsonEnvelope sjpEnvelope = envelopeFrom(
                metadataWithRandomUUID("stagingprosecutors.query.sjp.submission.v2"),
                createObjectBuilder()
                        .add("submissionId", submissionId.toString())
                        .add("oucode", ouCode)
                        .build()
        );
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = null;
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                ouCode,
                createArrayBuilder().build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);
        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonEnvelope jsonEnvelope = view.querySubmissionSjpV2(sjpEnvelope);
        assertThat(jsonEnvelope.metadata().asJsonObject().getString("name"), is("hmcts.cjs.query.sjp.submission.v2"));
    }

    @Test
    public void queriesForSubmissionUsingRepositoryReturnsNullPayloadWhenNotFound() {
        when(service.getSubmission(submissionId)).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = view.querySubmission(envelope);
        assertThat(jsonEnvelope.metadata().id(), is(envelope.metadata().id()));
        assertThat(jsonEnvelope.metadata().name(), is("hmcts.cjs.query.submission"));
        assertThat(jsonEnvelope.payload(), is(JsonValue.NULL));
    }

    @Test
    public void queriesForSubmissionV3UsingRepositoryForApplication() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);

        Submission submission = createSubmissionForApplication(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }

    @Test
    public void queriesForSJPSubmissionV3UsingRepositoryForApplication() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);

        Submission submission = createSubmissionForApplication(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.sjp.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionSjpV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }

    @Test
    public void queriesForSJPSubmissionV3UsingRepositoryForCaseWithSJPV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TVL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", true)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.sjp.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionSjpV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }

    @Test
    public void queriesForSJPSubmissionV3UsingRepositoryForCaseWithNonSJPV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TVL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", true)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }

    @Test
    public void queriesForSJPSubmissionV3UsingRepositoryForCaseWithSJPV3NonCpsOuCodeInHeaderMatchesProsecutingAuthorityOuCode() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TVL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", false)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }

    @Test
    public void queriesForSJPSubmissionV3UsingRepositoryForCaseWithNonSJPV3NonCpsOuCodeInHeaderMatchesProsecutingAuthorityOuCode() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TVL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", false)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        final JsonEnvelope jsonEnvelope = view.querySubmissionV3(requestEnvelope);

        assertThat(jsonEnvelope.metadata().name(), is(requestEnvelope.metadata().name()));

        assertThat(jsonEnvelope.payloadAsJsonObject().getString("id"), is(submissionId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("status"), is("PENDING"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("type"), is(submissionType.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("receivedAt"), is(receivedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("completedAt"), is(completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
    }


    @Test
    public void shouldThrowForbiddenExceptionIfOUCodeDoesNotMatchForSjpV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TFL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", false)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.sjp.submission.v3", payload);

        assertThrows(ForbiddenException.class, () -> view.querySubmissionSjpV3(requestEnvelope));
    }

    @Test
    public void shouldSendSubmissionStatusIfCPSOUCodeInHeaderDoesNotMatchWithProsecutingAuthorityForSjpV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TFL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));
        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", true)
                .build()));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.sjp.submission.v3", payload);

        view.querySubmissionSjpV3(requestEnvelope);
    }

    @Test
    public void shouldThrowForbiddenExceptionIfOUCodeDoesNotMatchForNonSjpV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TFL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", false)
                .build()));

        assertThrows(ForbiddenException.class, () -> view.querySubmissionV3(requestEnvelope));
    }

    @Test
    public void shouldSendSubmissionStatusIfCPSOUCodeInHeaderDoesNotMatchWithProsecutioingAuthorityForNonSjpV3() {
        final ZonedDateTime receivedAt = ZonedDateTime.now();
        final ZonedDateTime completedAt = ZonedDateTime.now().plusSeconds(50);
        final String ouCode = "TFL";

        final Submission submission = createSubmissionForCase(receivedAt, completedAt);

        when(service.getSubmission(submissionId)).thenReturn(Optional.of(submission));

        final JsonObject payload = createObjectBuilder()
                .add("submissionId", submissionId.toString())
                .add("oucode", ouCode)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("hmcts.cjs.query.submission.v3", payload);

        when(referenceDataService.getProsecutorByOuCode(any())).thenReturn(Optional.ofNullable(createObjectBuilder()
                .add("cpsFlag", true)
                .build()));

        view.querySubmissionV3(requestEnvelope);
    }


    private Submission createSubmissionForCase(final ZonedDateTime receivedAt, final ZonedDateTime completedAt) {
        final String ouCode = "TVL";
        Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                ouCode,
                createArrayBuilder().add("error").build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                null);
        submission.setCompletedAt(completedAt);
        return submission;
    }

    private Submission createSubmissionForApplication(final ZonedDateTime receivedAt, final ZonedDateTime completedAt) {
        final Submission submission = new Submission(
                submissionId,
                "PENDING",
                "caseUrn",
                null,
                createArrayBuilder().add("error").build(),
                createArrayBuilder().build(),
                submissionType,
                receivedAt,
                false,
                applicationId);
        submission.setCompletedAt(completedAt);
        return submission;
    }

}