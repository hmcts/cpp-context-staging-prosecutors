package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;

@ServiceComponent(QUERY_API)
public class ResultsQueryApi {

    private static final Logger LOGGER = getLogger(ResultsQueryApi.class);
    private static final String GET_RESULTS_API = "results.prosecutor-results";
    private static final String GET_RESULTS = "hmcts.results.v1";

    @Inject
    private Requester requester;

    @Handles("hmcts.results.v1")
    public JsonEnvelope getResults(final JsonEnvelope query) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Results requested: {}", query.toObfuscatedDebugString());
        }
        final JsonEnvelope resultsQueryEnvelope = envelopeFrom(
                metadataFrom(query.metadata())
                        .withName(GET_RESULTS_API)
                        .build(), query.payloadAsJsonObject());

        final JsonEnvelope resultsResponseEnvelope = requester.request(resultsQueryEnvelope);
        return envelopeFrom(
                metadataFrom(resultsResponseEnvelope.metadata())
                        .withName(GET_RESULTS)
                        .build(), flattenVerdictCodeInPayload(resultsResponseEnvelope.payloadAsJsonObject()));
    }

    private JsonObject flattenVerdictCodeInPayload(final JsonObject payload) {
        if (!(payload.get("hearingVenue") instanceof JsonObject)) {
            return payload;
        }
        final JsonObject hearingVenue = payload.getJsonObject("hearingVenue");
        if (!hearingVenue.containsKey("courtSessions")) {
            return payload;
        }
        return Json.createObjectBuilder(payload)
                .add("hearingVenue", transformHearingVenue(hearingVenue))
                .build();
    }

    private JsonObject transformHearingVenue(final JsonObject hearingVenue) {
        final JsonArrayBuilder courtSessionsBuilder = Json.createArrayBuilder();
        for (final JsonValue session : hearingVenue.getJsonArray("courtSessions")) {
            courtSessionsBuilder.add(transformCourtSession(session.asJsonObject()));
        }
        return Json.createObjectBuilder(hearingVenue)
                .add("courtSessions", courtSessionsBuilder)
                .build();
    }

    private JsonObject transformCourtSession(final JsonObject session) {
        if (!session.containsKey("defendants")) {
            return session;
        }
        final JsonArrayBuilder defendantsBuilder = Json.createArrayBuilder();
        for (final JsonValue defendant : session.getJsonArray("defendants")) {
            defendantsBuilder.add(transformDefendant(defendant.asJsonObject()));
        }
        return Json.createObjectBuilder(session)
                .add("defendants", defendantsBuilder)
                .build();
    }

    private JsonObject transformDefendant(final JsonObject defendant) {
        if (!defendant.containsKey("prosecutionCasesOrApplications")) {
            return defendant;
        }
        final JsonArrayBuilder casesBuilder = Json.createArrayBuilder();
        for (final JsonValue caseOrApp : defendant.getJsonArray("prosecutionCasesOrApplications")) {
            casesBuilder.add(transformCaseOrApplication(caseOrApp.asJsonObject()));
        }
        return Json.createObjectBuilder(defendant)
                .add("prosecutionCasesOrApplications", casesBuilder)
                .build();
    }

    private JsonObject transformCaseOrApplication(final JsonObject caseOrApp) {
        if (!caseOrApp.containsKey("offences")) {
            return caseOrApp;
        }
        final JsonArrayBuilder offencesBuilder = Json.createArrayBuilder();
        for (final JsonValue offence : caseOrApp.getJsonArray("offences")) {
            offencesBuilder.add(transformOffence(offence.asJsonObject()));
        }
        return Json.createObjectBuilder(caseOrApp)
                .add("offences", offencesBuilder)
                .build();
    }

    private JsonObject transformOffence(final JsonObject offence) {
        if (!offence.containsKey("verdict")) {
            return offence;
        }
        final String verdictCode = offence.getJsonObject("verdict").getString("verdictCode");
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : offence.entrySet()) {
            if (!"verdict".equals(entry.getKey())) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        builder.add("verdictCode", verdictCode);
        return builder.build();
    }
}