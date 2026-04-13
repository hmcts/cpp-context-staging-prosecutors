package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.DocumentBundleArrivedForUnbundling;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.APPLICATION_PDF;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.CASE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.ERROR_MESSAGE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_STORE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATETIME_FORMATTER;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATE_TIME;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECORD_DOCUMENT_UNBUNDLE_RESULT;

public class DocumentUnbundleResultBuilder {

    @Inject
    private EnvelopeHelper envelopeHelper;

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUnbundleResultBuilder.class);

    public JsonObject buildPayloadForFile(final FileHolder fileHolder) {

        return createObjectBuilder()
                .add(DOCUMENT_TYPE, fileHolder.getSectionName())
                .add(FILE_STORE_ID, fileHolder.getFileStoreId().toString())
                .add(FILE_TYPE, APPLICATION_PDF)
                .build();

    }


    public JsonEnvelope getFailedResult(final DocumentBundleArrivedForUnbundling unbundlingObject, String error) {
        final JsonObjectBuilder payloadBuilder = initializePayload(unbundlingObject);
        final JsonObject payload = payloadBuilder.add(ERROR_MESSAGE, error).build();

        return getJsonEnvelope(payload);
    }

    private JsonEnvelope getJsonEnvelope(final JsonObject payload) {
        LOGGER.info("Sending add material request to PCF with caseId : {}", payload.get("caseId"));
        final Metadata metadata = metadataBuilder()
                .withName(RECORD_DOCUMENT_UNBUNDLE_RESULT)
                .withId(UUID.randomUUID())
                .build();
        final Metadata jsonObject = metadataFrom(createObjectBuilder(metadata.asJsonObject()).build()).build();
        return envelopeHelper.withMetadataInPayload(envelopeFrom(jsonObject, payload));
    }

    public  JsonObjectBuilder initializePayload(final DocumentBundleArrivedForUnbundling unbundlingObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(CASE_ID, unbundlingObject.getCaseId().toString())
                .add(PROSECUTOR_DEFENDANT_ID, unbundlingObject.getProsecutorDefendantId())
                .add(RECEIVED_DATE_TIME, unbundlingObject.getReceivedDateTime().format(RECEIVED_DATETIME_FORMATTER));

        if (nonNull(unbundlingObject.getProsecutingAuthority())) {
            jsonObjectBuilder.add(PROSECUTING_AUTHORITY, unbundlingObject.getProsecutingAuthority());
        }

        return jsonObjectBuilder;
    }
}
