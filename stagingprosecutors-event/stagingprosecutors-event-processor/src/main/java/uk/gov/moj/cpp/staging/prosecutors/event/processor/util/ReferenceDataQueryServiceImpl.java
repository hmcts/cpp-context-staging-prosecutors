package uk.gov.moj.cpp.staging.prosecutors.event.processor.util;

import static java.lang.Boolean.TRUE;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReferenceDataQueryServiceImpl implements ReferenceDataQueryService {

    private static final String REFERENCEDATA_QUERY_PARENT_BUNDLE_SECTION = "referencedata.query.parent-bundle-section";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE = "referencedata.query.document-type-access-by-sectioncode";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataQueryServiceImpl.class);


    private static final String FIELD_CPS_BUNDLE_CODE = "cpsBundleCode";
    private static final String FIELD_CPS_ID = "id";
    private static final String FIELD_CPS_TARGET_SEC_CODE = "targetSectionCode";

    public static final String REFERENCEDATA_QUERY_PROSECUTOR_BY_CPSFLAG = "referencedata.query.get.prosecutor.by.cpsflag";
    public static final String CPS_FLAG = "cpsFlag";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;


    @Override
    public DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode) {



        final JsonEnvelope documentTypeAccessBySectionCodeEnvelope = envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE),
                createObjectBuilder().
                        add("sectionCode", sectionCode).build());

        final JsonValue response = requester.requestAsAdmin(documentTypeAccessBySectionCodeEnvelope, JsonObject.class).payload();

        DocumentTypeAccessReferenceData documentTypeAccessReferenceData = null;
        if (null != response && JsonValue.NULL != response) {
            documentTypeAccessReferenceData = asDocumentsMetadataRefData().apply(response);
        }

        return documentTypeAccessReferenceData;
    }


    @Override
    public ParentBundleSectionReferenceData getParentBundleSectionByCpsBundleCode(final Metadata metadata, final String cpsBundleCode) {
        final JsonEnvelope parentBundleSectionQueryEnvelope = envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_PARENT_BUNDLE_SECTION),
                createObjectBuilder().
                        add(FIELD_CPS_BUNDLE_CODE, cpsBundleCode));

        final JsonValue response = requester.requestAsAdmin(parentBundleSectionQueryEnvelope, JsonObject.class).payload();

        ParentBundleSectionReferenceData parentBundleSectionReferenceData = null;
        if (null != response) {
            parentBundleSectionReferenceData = asParentBundleSectionRefData().apply(response);
        }

        return parentBundleSectionReferenceData;
    }

    public static Function<JsonValue, DocumentTypeAccessReferenceData> asDocumentsMetadataRefData() {
        return jsonValue -> {
            try {
                return OBJECT_MAPPER.readValue(jsonValue.toString(), DocumentTypeAccessReferenceData.class);
            } catch (IOException e) {
                LOGGER.error("Unable to unmarshal DocumentTypeAccessReferenceData.", e);
                return null;
            }
        };
    }

    public static Function<JsonValue, ParentBundleSectionReferenceData> asParentBundleSectionRefData() {

        return jsonValue -> {
            final JsonObject refDataObject = (JsonObject) jsonValue;
            final ParentBundleSectionReferenceData.Builder builder = ParentBundleSectionReferenceData.parentBundleSectionReferenceData();

            buildParentBundleSection(builder, refDataObject);
            return builder.build();
        };
    }

    public Optional<JsonArray> getCPSProsecutors(final JsonEnvelope event, final Requester requester) {

        LOGGER.info(" Calling {} to get prosecutors with cpsFlag true", REFERENCEDATA_QUERY_PROSECUTOR_BY_CPSFLAG);

        final JsonObject payload = createObjectBuilder()
                .add(CPS_FLAG, TRUE)
                .build();

        final JsonEnvelope response = requester.request(envelop(payload)
                .withName(REFERENCEDATA_QUERY_PROSECUTOR_BY_CPSFLAG) //TODO check correct request name used??
                .withMetadataFrom(event));

        if (JsonValue.NULL.equals(response.payload())) {
            return Optional.empty();
        }

        return Optional.of(response.payloadAsJsonObject().getJsonArray("prosecutors"));
    }


    private static void buildParentBundleSection(ParentBundleSectionReferenceData.Builder builder, JsonObject refDataObject) {
        getStringFromJson(FIELD_CPS_ID, refDataObject).map(UUID::fromString).ifPresent(builder::withId);
        getStringFromJson(FIELD_CPS_BUNDLE_CODE, refDataObject).ifPresent(builder::withCpsBundleCode);
        getStringFromJson(FIELD_CPS_TARGET_SEC_CODE, refDataObject).ifPresent(builder::withTargetSectionCode);
    }

    private static Optional<String> getStringFromJson(final String name, final JsonObject jsonObject) {
        return Optional.ofNullable(jsonObject.getString(name, null));
    }

}
