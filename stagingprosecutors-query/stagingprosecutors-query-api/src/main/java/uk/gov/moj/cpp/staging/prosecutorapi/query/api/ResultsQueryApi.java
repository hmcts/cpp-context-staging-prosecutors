package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.api.converter.ResultsV1ResponseTransformer;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(QUERY_API)
public class ResultsQueryApi {

    private static final Logger LOGGER = getLogger(ResultsQueryApi.class);
    private static final String GET_RESULTS_API = "results.prosecutor-results";
    private static final String GET_RESULTS_V1 = "hmcts.results.v1";
    private static final String GET_RESULTS_V2 = "hmcts.results.v2";

    @Inject
    private Requester requester;

    @Inject
    private ResultsV1ResponseTransformer resultsV1ResponseTransformer;

    @Handles("hmcts.results.v1")
    public JsonEnvelope getResults(final JsonEnvelope query) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Results v1 requested: {}", query.toObfuscatedDebugString());
        }
        final JsonEnvelope resultsResponseEnvelope = requestResults(query);
        return envelopeFrom(
                metadataFrom(resultsResponseEnvelope.metadata())
                        .withName(GET_RESULTS_V1)
                        .build(),
                resultsV1ResponseTransformer.transform(resultsResponseEnvelope.payloadAsJsonObject()));
    }

    @Handles("hmcts.results.v2")
    public JsonEnvelope getResultsV2(final JsonEnvelope query) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Results v2 requested: {}", query.toObfuscatedDebugString());
        }
        final JsonEnvelope resultsResponseEnvelope = requestResults(query);
        return envelopeFrom(
                metadataFrom(resultsResponseEnvelope.metadata())
                        .withName(GET_RESULTS_V2)
                        .build(),
                resultsResponseEnvelope.payloadAsJsonObject());
    }

    private JsonEnvelope requestResults(final JsonEnvelope query) {
        final JsonEnvelope resultsQueryEnvelope = envelopeFrom(
                metadataFrom(query.metadata())
                        .withName(GET_RESULTS_API)
                        .build(), query.payloadAsJsonObject());
        return requester.request(resultsQueryEnvelope);
    }
}
