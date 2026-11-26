package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;

import javax.inject.Inject;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@ServiceComponent(QUERY_API)
public class SubmissionQueryApi {

    @Inject
    private SubmissionQueryView submissionQueryView;

    @Handles("hmcts.cjs.submission")
    public JsonEnvelope querySubmission(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);


        final JsonEnvelope queryEnvelop = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.submission"), envelope.payloadAsJsonObject());
        return submissionQueryView.querySubmission(queryEnvelop);
    }


    @Handles("hmcts.cjs.submission.v2")
    public JsonEnvelope querySubmissionV2(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);

        return envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.submission.v2")
                        .build(), submissionQueryView.querySubmissionV2(envelope.payloadAsJsonObject()));
    }

    @Handles("hmcts.cjs.submission.v3")
    public JsonEnvelope querySubmissionV3(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);

        final JsonEnvelope queryEnvelop = envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.submission.v3")
                        .build(), envelope.payloadAsJsonObject());

        return submissionQueryView.querySubmissionV3(queryEnvelop);
    }

    @Handles("hmcts.cps.submission.v1")
    public JsonEnvelope cpsQuerySubmissionV1(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);

        final JsonEnvelope queryEnvelop = envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cps.query.submission.v1")
                        .build(), envelope.payloadAsJsonObject());

        return submissionQueryView.cpsQuerySubmissionV1(queryEnvelop);
    }

    @Handles("hmcts.cjs.sjp.submission.v2")
    public JsonEnvelope querySubmissionSjpV2(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);

        final JsonEnvelope queryEnvelop = envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.sjp.submission.v2")
                        .build(), envelope.payloadAsJsonObject());

        return submissionQueryView.querySubmissionSjpV2(queryEnvelop);
    }

    @Handles("hmcts.cjs.sjp.submission.v3")
    public JsonEnvelope querySubmissionSjpV3(final JsonEnvelope envelope) {
        validateSubmissionId(envelope);

        final JsonEnvelope queryEnvelop = envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("hmcts.cjs.query.sjp.submission.v3")
                        .build(), envelope.payloadAsJsonObject());

        return submissionQueryView.querySubmissionSjpV3(queryEnvelop);
    }

    @SuppressWarnings("squid:S1166")
    private void validateSubmissionId(final JsonEnvelope envelope) {
        final String submissionId = envelope.payloadAsJsonObject().getString("submissionId");
        try {
            fromString(submissionId);
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException(format("Specified string %s, is not valid UUID", submissionId));
        }
    }


}
