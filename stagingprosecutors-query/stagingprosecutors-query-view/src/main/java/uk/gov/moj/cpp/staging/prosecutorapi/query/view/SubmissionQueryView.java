package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.SubmissionService;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.ForbiddenException;

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
    private static final ZoneId UTC = ZoneId.of("UTC");
    // J17 (Hibernate 5) rendering: seconds omitted when zero, fraction in 3/6/9-digit groups.
    private static final DateTimeFormatter TO_MINUTES = utcRegionFormatter(false, 0);
    private static final DateTimeFormatter TO_SECONDS = utcRegionFormatter(true, 0);
    private static final DateTimeFormatter TO_MILLIS = utcRegionFormatter(true, 3);
    private static final DateTimeFormatter TO_MICROS = utcRegionFormatter(true, 6);
    private static final DateTimeFormatter TO_NANOS = utcRegionFormatter(true, 9);

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
                                    .add(RECEIVED_AT, inUtcRegion(submission.getReceivedAt()));

                            ofNullable(submissionOptional.get().getCompletedAt()).
                                    ifPresent(completedAt -> result.add(COMPLETED_AT, inUtcRegion(completedAt)));
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
                                    .add(RECEIVED_AT, inUtcRegion(submission.getReceivedAt()));

                    ofNullable(submissionOptional.get().getCompletedAt()).
                            ifPresent(completedAt -> result.add(COMPLETED_AT, inUtcRegion(completedAt)));
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
                                    .add(RECEIVED_AT, inUtcRegion(submission.getReceivedAt()));

                            ofNullable(submissionOptional.get().getCompletedAt()).
                                    ifPresent(completedAt -> result.add(COMPLETED_AT, inUtcRegion(completedAt)));
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
                                    .add(RECEIVED_AT, inUtcRegion(submission.getReceivedAt()));

                    ofNullable(submissionOptional.get().getCompletedAt()).
                            ifPresent(completedAt -> result.add(COMPLETED_AT, inUtcRegion(completedAt)));
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

    // Hibernate 6 reads the timestamps back as ZoneOffset.UTC ("...Z"); keep the J17 (Hibernate 5) "...Z[UTC]" form.
    private static String inUtcRegion(final ZonedDateTime dateTime) {
        final ZonedDateTime utc = dateTime.withZoneSameInstant(UTC);
        final int nanos = utc.getNano();
        final DateTimeFormatter formatter;
        if (nanos == 0) {
            formatter = utc.getSecond() == 0 ? TO_MINUTES : TO_SECONDS;
        } else if (nanos % 1_000_000 == 0) {
            formatter = TO_MILLIS;
        } else if (nanos % 1_000 == 0) {
            formatter = TO_MICROS;
        } else {
            formatter = TO_NANOS;
        }
        return formatter.format(utc);
    }

    private static DateTimeFormatter utcRegionFormatter(final boolean withSeconds, final int fractionDigits) {
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .append(ISO_LOCAL_DATE)
                .appendLiteral('T')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2);
        if (withSeconds) {
            builder.appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2);
        }
        if (fractionDigits > 0) {
            builder.appendFraction(NANO_OF_SECOND, fractionDigits, fractionDigits, true);
        }
        return builder.appendOffsetId()
                .appendLiteral('[').appendZoneRegionId().appendLiteral(']')
                .toFormatter()
                .withZone(UTC);
    }

}
