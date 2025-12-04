package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ProsecutorCaseReferenceUtil.getProsecutorCaseReference;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDocument;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsMaterialSubmitted;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import org.slf4j.Logger;
@SuppressWarnings({"squid:S3655","squid:CallToDeprecatedMethod"})
@ServiceComponent(EVENT_PROCESSOR)
public class MaterialSubmittedProcessor {

    public static final String CASE_LEVEL = "Case level";
    public static final String DEFENDANT_LEVEL = "Defendant level";

    private  static final String CASE_MANAGEMENT_SECTION = "Case Management";
    private  static final String CASE_ID = "caseId";
    private  static final String APPLICATION_ID = "applicationId";

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    private Sender sender;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    ReferenceDataQueryService referenceDataQueryService;

    @Handles("stagingprosecutors.event.material-submitted")
    public void onMaterialSubmitted(final Envelope<MaterialSubmitted> materialSubmittedEnvelope) {

        final MaterialSubmitted materialSubmitted = materialSubmittedEnvelope.payload();

        final String prosecutorCaseReference = getProsecutorCaseReference(
                materialSubmitted.getProsecutingAuthority(),
                materialSubmitted.getCaseUrn());
        final UUID caseId = systemIdMapperService.getCppCaseIdFor(prosecutorCaseReference);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add("isCpsCase", materialSubmitted.getIsCpsCase())
                .add("material", createObjectBuilder()
                        .add("documentType", materialSubmitted.getMaterialType())
                        .add("fileStoreId", materialSubmitted.getMaterialId().toString())
                        .build()
                );

        ofNullable(materialSubmitted.getProsecutingAuthority()).ifPresent(prosecutingAuthority -> payloadBuilder.add("prosecutingAuthority", prosecutingAuthority));
        ofNullable(materialSubmitted.getDefendantId()).ifPresent(id -> payloadBuilder.add("prosecutorDefendantId", id));

        final Metadata metadata = metadataFrom(materialSubmittedEnvelope.metadata())
                .withName("prosecutioncasefile.add-material")
                .build();

        final String submissionId = materialSubmitted.getSubmissionId().toString();

        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(withSubmissionId(metadata, submissionId), payloadBuilder.build()));

        sender.sendAsAdmin(jsonEnvelope);
    }

    @Handles("stagingprosecutors.event.material-submitted-v3")
    public void materialSubmittedV3(final Envelope<MaterialSubmittedV3> materialSubmittedEnvelope) {

        String prosecutorCaseReference = null;

        final MaterialSubmittedV3 materialSubmitted = materialSubmittedEnvelope.payload();

        JsonObject submittedPayload = objectToJsonObjectConverter.convert(materialSubmitted);

        submittedPayload = removeProperty(submittedPayload, "submissionStatus");
        submittedPayload = JsonObjects.createObjectBuilder(submittedPayload)
                .add("receivedDateTime", materialSubmittedEnvelope.metadata().createdAt().orElse(ZonedDateTime.now()).toString())
                .build();

        final Optional<ProsecutionCaseSubject> prosecutionCaseSubject = ofNullable(materialSubmitted.getProsecutionCaseSubject());
        final String submissionId = ofNullable(materialSubmitted.getSubmissionId()).map(UUID::toString).orElse(null);

        if (prosecutionCaseSubject.isPresent()) {
            prosecutorCaseReference = getProsecutorCaseReference(
                    prosecutionCaseSubject.get().getProsecutingAuthority(),
                    prosecutionCaseSubject.get().getCaseUrn());
            final UUID caseId = systemIdMapperService.getCaseIdForMaterialSubmission(prosecutorCaseReference);

            submittedPayload = JsonObjects.createObjectBuilder(submittedPayload).add(CASE_ID, caseId.toString()).build();

            final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(withSubmissionId(metadataFrom(materialSubmittedEnvelope.metadata())
                    .withName("prosecutioncasefile.add-material-v2").build(), submissionId),submittedPayload));

           sender.sendAsAdmin(jsonEnvelope);

        } else {
            final Optional<CourtApplicationSubject> courtApplicationSubject = ofNullable(materialSubmitted.getCourtApplicationSubject());
            if (courtApplicationSubject.isPresent()) {
                submittedPayload = removeProperty(submittedPayload, "isCpsCase");
                submittedPayload = JsonObjects.createObjectBuilder(submittedPayload).add(APPLICATION_ID, courtApplicationSubject.get().getCourtApplicationId().toString()).build();
            }

            final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(withSubmissionId(metadataFrom(materialSubmittedEnvelope.metadata())
                    .withName("prosecutioncasefile.add-application-material-v2").build(), submissionId), submittedPayload));

            sender.sendAsAdmin(jsonEnvelope);
        }
    }

    @Handles("stagingprosecutors.event.cps-material-submitted")
    public void onCpsMaterialSubmitted(final Envelope<CpsMaterialSubmitted> materialSubmittedEnvelope) {

        final CpsMaterialSubmitted materialSubmitted = materialSubmittedEnvelope.payload();

        final UUID caseId = systemIdMapperService.getCppCaseIdFor(materialSubmitted.getUrn());
        final String submissionId = materialSubmitted.getSubmissionId().toString();

        for (final CpsDocument document : materialSubmitted.getDocuments()) {
            if (isCaseLevelDocument(materialSubmittedEnvelope.metadata(), document)) {
                callAddCpsMaterial(materialSubmittedEnvelope, caseId, submissionId, document, null);
            } else {
                for (final CpsDefendant defendant : materialSubmitted.getDefendants()) {
                    callAddCpsMaterial(materialSubmittedEnvelope, caseId, submissionId, document, defendant);
                }
            }
        }
    }

    private void callAddCpsMaterial(final Envelope<CpsMaterialSubmitted> materialSubmittedEnvelope, final UUID caseId, final String submissionId, final CpsDocument document, final CpsDefendant defendant) {
        final String materialType = isPetForm(document) ? CASE_MANAGEMENT_SECTION : document.getMaterialType().toString();
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add("material", createObjectBuilder()
                        .add("documentType", materialType)
                        .add("fileStoreId", document.getFileStoreId().toString())
                        .build()
                )
                .add("cmsDocumentIdentifier", createObjectBuilder()
                        .add("materialType", ofNullable(document.getMaterialType()).orElse(null))
                        .add("documentId", document.getDocumentId())
                        .build()
                )
                .add("receivedDateTime", materialSubmittedEnvelope.metadata().createdAt().orElse(ZonedDateTime.now()).toString());

        if(nonNull(defendant) && ofNullable(defendant.getAsn()).isPresent()){
            payloadBuilder.add("prosecutorDefendantId", ofNullable(defendant.getAsn()).orElse(null));
        }

        final Metadata metadata = metadataFrom(materialSubmittedEnvelope.metadata())
                .withName("prosecutioncasefile.add-cps-material")
                .build();

        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(withSubmissionId(metadata, submissionId), payloadBuilder.build()));

        sender.sendAsAdmin(jsonEnvelope);
    }

    private boolean isCaseLevelDocument(final Metadata metadata, final CpsDocument document){
        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = getDocumentTypeAccessRefData(metadata, ofNullable(document.getMaterialType()).orElse(0).toString());
        return documentTypeAccessReferenceData != null && CASE_LEVEL.equals(documentTypeAccessReferenceData.getDocumentCategory());
    }

    private DocumentTypeAccessReferenceData getDocumentTypeAccessRefData(final Metadata metadata, final String materialType) {
        final ParentBundleSectionReferenceData parentBundleSectionByCpsBundleCode =
                referenceDataQueryService.getParentBundleSectionByCpsBundleCode(metadata, materialType);

        if (parentBundleSectionByCpsBundleCode != null && parentBundleSectionByCpsBundleCode.getTargetSectionCode() != null) {
            return referenceDataQueryService.getDocumentTypeAccessBySectionCode(metadata, parentBundleSectionByCpsBundleCode.getTargetSectionCode());
        }
        return null;
    }

    private boolean isPetForm(final CpsDocument document) {
        return ofNullable(document.getMaterialType()).get() == 0 && ofNullable(document.getFileName()).orElse("").toLowerCase().startsWith("magistrates' court pet form");
    }

    private Metadata withSubmissionId(final Metadata metadata, final String submissionId) {
        return metadataFrom(JsonObjects.createObjectBuilder(metadata.asJsonObject()).add("submissionId", submissionId).build()).build();
    }


    public static JsonObject removeProperty(JsonObject origin, String key) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            if (!entry.getKey().equals(key)) {
                if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                    builder.add(entry.getKey(), removeProperty(origin.getJsonObject(entry.getKey()), key));
                } else {
                    builder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build();
    }

}
