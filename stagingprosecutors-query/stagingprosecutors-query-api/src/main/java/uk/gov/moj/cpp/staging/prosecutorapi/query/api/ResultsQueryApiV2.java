package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(QUERY_API)
public class ResultsQueryApiV2 {

    private static final Logger LOGGER = getLogger(ResultsQueryApiV2.class);
    private static final String GET_RESULTS_API = "results.prosecutor-results";
    private static final String GET_RESULTS_V2 = "hmcts.results.v2";

    @Inject
    private Requester requester;

    @Handles("hmcts.results.v2")
    public JsonEnvelope getResults(final JsonEnvelope query) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Results v2 requested: {}", query.toObfuscatedDebugString());
        }
        final JsonEnvelope resultsQueryEnvelope = envelopeFrom(
                metadataFrom(query.metadata())
                        .withName(GET_RESULTS_API)
                        .build(), query.payloadAsJsonObject());

        final JsonEnvelope resultsResponseEnvelope = requester.request(resultsQueryEnvelope);
        return envelopeFrom(
                metadataFrom(resultsResponseEnvelope.metadata())
                        .withName(GET_RESULTS_V2)
                        .build(), resultsResponseEnvelope.payloadAsJsonObject());
    }
}
