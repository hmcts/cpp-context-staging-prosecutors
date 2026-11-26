package uk.gov.moj.cpp.staging.prosecutors.command.handler.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Jurisdiction;


public class ReferenceDataServiceImpl implements ReferenceDataService{

    private static final String REFERENCEDATA_QUERY_APPLICATION_TYPES = "referencedata.query.application-types";
    private static final String REFERENCEDATA_QUERY_PUBLIC_HOLIDAYS_NAME = "referencedata.query.public-holidays";
    private static final String PUBLIC_HOLIDAYS = "publicHolidays";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FIELD_APPLICATION_TYPES = "courtApplicationTypes";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Override
    public Optional<CourtApplicationType> retrieveApplicationTypes(final String code) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_APPLICATION_TYPES), createObjectBuilder());
        return  requester.requestAsAdmin(envelope, JsonObject.class)
                .payload()
                .getJsonArray(FIELD_APPLICATION_TYPES)
                .stream()
                .map(JsonObject.class::cast)
                .filter(jsonObject -> code.equals(jsonObject.getString("code", null)))
                .findFirst()
                .map(jsonObject -> CourtApplicationType.courtApplicationType()
                        .withCategoryCode(jsonObject.getString("categoryCode"))
                        .withCode(jsonObject.getString("code"))
                        .withJurisdiction(Jurisdiction.valueOf(jsonObject.getString("jurisdiction")))
                        .build());
    }

    @Override
    public List<LocalDate> getPublicHolidays(final LocalDate fromDate,
                                                 final LocalDate toDate) {

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_PUBLIC_HOLIDAYS_NAME);

        final JsonObject params = getParams(fromDate, toDate);

        final Envelope<JsonObject> jsonObjectEnvelope = requester.requestAsAdmin(envelopeFrom(metadataBuilder, params), JsonObject.class);

        return transform(jsonObjectEnvelope);
    }

    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }

    private JsonObject getParams(final LocalDate fromDate,
                                 final LocalDate toDate) {
        return createObjectBuilder()
                .add("division", "england-and-wales")
                .add("dateFrom", fromDate.toString())
                .add("dateTo", toDate.toString())
                .build();
    }

    private List<LocalDate> transform(final Envelope<JsonObject> envelope) {
        final List<LocalDate> publicHolidays = new ArrayList();
        final JsonObject payload = envelope.payload();
        if (payload.containsKey(PUBLIC_HOLIDAYS)) {
            final JsonArray jsonArray = payload.getJsonArray(PUBLIC_HOLIDAYS);
            if (!jsonArray.isEmpty()) {
                final List<JsonObject> publicHolidaysArray = jsonArray.getValuesAs(JsonObject.class);
                for (final JsonObject pd : publicHolidaysArray) {
                    publicHolidays.add(LocalDate.parse(pd.getString("date"), DATE_FORMATTER));
                }
            }
        }
        return publicHolidays;
    }
}
