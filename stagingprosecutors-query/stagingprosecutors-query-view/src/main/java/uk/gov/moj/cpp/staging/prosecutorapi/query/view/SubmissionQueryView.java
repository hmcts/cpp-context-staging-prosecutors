package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.SubmissionService;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.StringUtils;

public class SubmissionQueryView {

    private static final String ERRORS_FIELD = "errors";
    private static final String WARNINGS = "warnings";
    private static final String SUBMISSION_ID = "submissionId";
    private static final String RECEIVED_AT = "receivedAt";
    private static final String COMPLETED_AT= "completedAt";
    private static final String STATUS = "status";
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String OU_CODE = "oucode";

    @Inject
    private Enveloper enveloper;

    @Inject
    private SubmissionService service;

    @Inject
    private ReferenceDataService referenceDataService;


    public JsonEnvelope querySubmission(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        final JsonObject payload = submissionOptional
                .map(submission -> {
                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add(ID, submission.getSubmissionId().toString())
                                    .add(STATUS, submission.getSubmissionStatus())
                                    .add(TYPE, submission.getType().toString())
                                    .add(WARNINGS, submission.getWarnings())
                                    .add(ERRORS_FIELD, submission.getErrors())
                                    .add(RECEIVED_AT, submission.getReceivedAt().toString());

                            ofNullable(submissionOptional.get().getCompletedAt()).
                                    ifPresent(completedAt -> result.add(COMPLETED_AT, completedAt.toString()));
                            return result.build();
                        }
                )
                .orElse(null);


        return envelopeFrom(metadataFrom(envelope.metadata())
                .withName("hmcts.cjs.query.submission"), payload);
    }

    public JsonObject querySubmissionV2(final JsonObject jsonObject) {
        final UUID submissionId = fromString(jsonObject
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);
        final boolean isValidOuCode = submissionOptional
                .filter(submission -> submission.getApplicationId() == null)
                .map(submission -> submission.getOuCode().equalsIgnoreCase(jsonObject.getString(OU_CODE, null))).orElse(true);

        if (!isValidOuCode) {
            throw new ForbiddenException();
        }

        return submissionOptional
                .map(submission -> {
                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add(ID, submission.getSubmissionId().toString())
                                    .add(STATUS, submission.getSubmissionStatus())
                                    .add(TYPE, submission.getType().toString())
                                    .add("caseWarnings", submission.getCaseWarnings())
                                    .add("caseErrors", submission.getCaseErrors())
                                    .add("defendantWarnings", submission.getDefendantWarnings())
                                    .add("defendantErrors", submission.getDefendantErrors())
                                    .add(ERRORS_FIELD, submission.getErrors())
                                    .add(WARNINGS, submission.getWarnings())
                                    .add(RECEIVED_AT, submission.getReceivedAt().toString());

                    ofNullable(submissionOptional.get().getCompletedAt()).
                            ifPresent(completedAt -> result.add(COMPLETED_AT, completedAt.toString()));
                            return result.build();
                        }
                )
                .orElse(null);

    }

    public JsonEnvelope cpsQuerySubmissionV1(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);


        final JsonObject payload = submissionOptional
                .map(submission -> {
                            if (!submission.isCpsCase()) {
                                throw new ForbiddenException("Forbidden to query for the specified submissionId!");
                            }

                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add(ID, submission.getSubmissionId().toString())
                                    .add(STATUS, submission.getSubmissionStatus())
                                    .add(TYPE, submission.getType().toString())
                                    .add("caseWarnings", submission.getCaseWarnings())
                                    .add("caseErrors", submission.getCaseErrors())
                                    .add("defendantWarnings", submission.getDefendantWarnings())
                                    .add("defendantErrors", submission.getDefendantErrors())
                                    .add(ERRORS_FIELD, submission.getErrors())
                                    .add(RECEIVED_AT, submission.getReceivedAt().toString());

                            ofNullable(submissionOptional.get().getCompletedAt()).
                                    ifPresent(completedAt -> result.add(COMPLETED_AT, completedAt.toString()));
                            return result.build();
                        }
                )
                .orElse(null);

        return envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cps.query.submission.v1")
                        .build(), payload);
    }

    public JsonEnvelope querySubmissionSjpV2(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        final boolean isValidOuCode = submissionOptional
                .filter(submission -> submission.getApplicationId() == null)
                .map(submission -> StringUtils.equals(envelope.payloadAsJsonObject().getString(OU_CODE), submission.getOuCode()))
                .orElse(true);

        if (!isValidOuCode) {
            throw new ForbiddenException();
        }

        final JsonObject payload = submissionOptional
                .map(submission -> {
                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add(ID, submission.getSubmissionId().toString())
                                    .add(STATUS, submission.getSubmissionStatus())
                                    .add(TYPE, submission.getType().toString())
                                    .add(WARNINGS, submission.getWarnings())
                                    .add(ERRORS_FIELD, submission.getErrors())
                                    .add(RECEIVED_AT, submission.getReceivedAt().toString());

                    ofNullable(submissionOptional.get().getCompletedAt()).
                            ifPresent(completedAt -> result.add(COMPLETED_AT, completedAt.toString()));
                            return result.build();
                        }
                )
                .orElse(null);

        return envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.sjp.submission.v2")
                        .build(), payload);
    }

    public JsonEnvelope querySubmissionV3(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        validateOuCode(submissionOptional, envelope);

        return envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.submission.v3")
                        .build(), createPayload(submissionOptional));
    }

    public JsonEnvelope querySubmissionSjpV3(final JsonEnvelope envelope) {
        final UUID submissionId = fromString(envelope.payloadAsJsonObject()
                .getString(SUBMISSION_ID));

        final Optional<Submission> submissionOptional = service.getSubmission(submissionId);

        validateOuCode(submissionOptional, envelope);

        return envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.sjp.submission.v3")
                        .build(), createPayload(submissionOptional));
    }

    private void validateOuCode(final Optional<Submission> submissionOptional, final JsonEnvelope envelope) {

        boolean isCpsProsecutor = false;

        if (submissionOptional.isPresent() && isNull(submissionOptional.get().getApplicationId())) {
            final Optional<JsonObject> prosecutor = referenceDataService.getProsecutorByOuCode(envelope.payloadAsJsonObject().getString(OU_CODE));
            if (prosecutor.isPresent()) {
                isCpsProsecutor = prosecutor.get().getBoolean("cpsFlag");
            }

            final boolean isValidOuCode = submissionOptional
                    .filter(submission -> submission.getApplicationId() == null)
                    .map(submission -> submission.getOuCode().equalsIgnoreCase(envelope.payloadAsJsonObject().getString(OU_CODE, null))).orElse(true);

            if (!isValidOuCode && !isCpsProsecutor) {
                throw new ForbiddenException();
            }
        }
    }

    private JsonObject createPayload(final Optional<Submission> submissionOptional) {
        return submissionOptional
                .map(submission -> {
                            final JsonObjectBuilder result = createObjectBuilder()
                                    .add(ID, submission.getSubmissionId().toString())
                                    .add(STATUS, submission.getSubmissionStatus())
                                    .add(TYPE, submission.getType().toString())
                                    .add(WARNINGS, submission.getWarnings())
                                    .add(ERRORS_FIELD, submission.getErrors())
                                    .add(RECEIVED_AT, submission.getReceivedAt().withZoneSameLocal(ZoneId.of("Z")).toString());

                            ofNullable(submissionOptional.get().getCompletedAt()).
                                    ifPresent(completedAt -> result.add(COMPLETED_AT, completedAt.withZoneSameLocal(ZoneId.of("Z")).toString()));
                            return result.build();
                        }
                )
                .orElse(null);
    }

}
