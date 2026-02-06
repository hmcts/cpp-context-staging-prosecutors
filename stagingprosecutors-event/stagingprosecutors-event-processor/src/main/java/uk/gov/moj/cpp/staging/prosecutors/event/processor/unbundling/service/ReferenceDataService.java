package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.BundleSectionsNotFoundException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.PDFBundleDetails;

import javax.inject.Inject;
import javax.json.JsonObject;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

public class ReferenceDataService {
    public static final String REFERENCE_DATA_GET_DOCUMENT_BUNDLE = "referencedata.query.parent-bundle-section";
    public static final String CPS_BUNDLE_CODE = "cpsBundleCode";
    private static final String EXCEPTION_MESSAGE_STR = "Not found bundle sections for cpsBundleCode %s";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public PDFBundleDetails getPDFBundleDetails(final int cmsMaterialType) {

        final JsonObject payload = createObjectBuilder().add(CPS_BUNDLE_CODE, String.valueOf(cmsMaterialType)).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCE_DATA_GET_DOCUMENT_BUNDLE),
                payload);

        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);
        final JsonObject responseJson = jsonResultEnvelope.payloadAsJsonObject();

        if (isNull(responseJson)) {
            throw new BundleSectionsNotFoundException(String.format(EXCEPTION_MESSAGE_STR, CPS_BUNDLE_CODE));
        }
        return jsonObjectToObjectConverter.convert(responseJson, PDFBundleDetails.class);
    }
}
