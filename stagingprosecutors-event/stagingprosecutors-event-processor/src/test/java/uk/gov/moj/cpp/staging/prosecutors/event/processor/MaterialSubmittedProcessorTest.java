package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3.materialSubmittedV3;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.MaterialSubmittedProcessor.CASE_LEVEL;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.MaterialSubmittedProcessor.DEFENDANT_LEVEL;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted.materialSubmitted;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.SUBMISSION_ID;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.URN;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDocument;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsMaterialSubmitted;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialSubmittedProcessorTest {

    @InjectMocks
    private MaterialSubmittedProcessor processor;

    @Mock
    private UtcClock utcClock;

    @Mock
    private Sender sender;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static final String DOCUMENT_TYPE = "SJPN";

    @BeforeEach
    public void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
        ReflectionUtil.setField(processor, "objectToJsonObjectConverter", objectToJsonObjectConverter);
    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialWhenMaterialSubmitted() {
        final String defendantId = randomUUID().toString();
        final String documentType = "SJPN";
        final UUID materialId = randomUUID();

        final MaterialSubmitted materialSubmitted = materialSubmitted()
                .withCaseUrn(URN)
                .withDefendantId(defendantId)
                .withMaterialType(documentType)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(PENDING)
                .withMaterialId(materialId)
                .withIsCpsCase(false)
                .build();

        final UUID messageId = randomUUID();
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        final Envelope<MaterialSubmitted> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.material-submitted")
                        .withId(messageId)
                        .build(),
                materialSubmitted
        );

        final UUID caseFileId = randomUUID();

        when(systemIdMapperService.getCppCaseIdFor(PROSECUTING_AUTHORITY + ":" + URN)).thenReturn(caseFileId);

        processor.onMaterialSubmitted(materialSubmittedEnvelope);

        verify(sender, times(1)).sendAsAdmin(envelope);

        final JsonEnvelope actualEnvelope = captor.getValue();

        final Metadata actualMetadata = actualEnvelope.metadata();
        assertThat(actualMetadata.name(), is("prosecutioncasefile.add-material"));
        assertThat(actualMetadata.asJsonObject().getString("submissionId"), is(SUBMISSION_ID.toString()));

        final JsonObject payload = actualEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseFileId.toString()));
        assertThat(payload.getString("prosecutingAuthority"), is(PROSECUTING_AUTHORITY));
        assertThat(payload.getString("prosecutorDefendantId"), is(defendantId));
        final JsonObject material = payload.getJsonObject("material");
        assertThat(material.getString("documentType"), is(documentType));
        assertThat(material.getString("fileStoreId"), is(materialId.toString()));

    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialOnceWhenCpsMaterialSubmittedWithCaseLevelDoc(){
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID materialId = randomUUID();
        final Integer compassCaseId = (int) Math.random();
        final Integer transactionId = (int) Math.random();
        final Integer documentType = 5;
        final String responseEmail = "responseEmail@mail.com";

        final String documentCategory = CASE_LEVEL;

        final CpsDefendant defendant1 = CpsDefendant.cpsDefendant().withAsn(defendantId1.toString()).build();
        final CpsDefendant defendant2 = CpsDefendant.cpsDefendant().withAsn(defendantId2.toString()).build();

        final CpsMaterialSubmitted cpsMaterialSubmitted = CpsMaterialSubmitted.cpsMaterialSubmitted()
                .withCompassCaseId(compassCaseId)
                .withDefendants(Stream.of(defendant1,defendant2).collect(Collectors.toList()))
                .withDocuments(singletonList(CpsDocument.cpsDocument().withDocumentId(UUID.randomUUID().toString()).withFileStoreId(materialId).withMaterialType(documentType).build()))
                .withResponseEmail(responseEmail)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus.PENDING)
                .withTransactionID(transactionId)
                .withUrn(URN)
                .build();

        final UUID messageId = randomUUID();
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        final Envelope<CpsMaterialSubmitted> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.cps-material-submitted")
                        .withId(messageId)
                        .build(),
                cpsMaterialSubmitted
        );

        final UUID caseFileId = randomUUID();

        when(systemIdMapperService.getCppCaseIdFor(URN)).thenReturn(caseFileId);
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(),any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData().withTargetSectionCode("01").build());
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(),any())).thenReturn(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData().withDocumentCategory(documentCategory).build());

        processor.onCpsMaterialSubmitted(materialSubmittedEnvelope);

        verify(sender, times(1)).sendAsAdmin(envelope);

        verifyPayloadForCaseLevelDocumentCategory(defendantId1, defendantId2, materialId, caseFileId, captor.getValue());
    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialForEachDefendantWhenCpsMaterialSubmittedWithDefendantLevelDoc() {
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID materialId = randomUUID();
        final Integer compassCaseId = (int) Math.random();
        final Integer transactionId = (int) Math.random();
        final Integer documentType = 5;
        final String responseEmail = "responseEmail@mail.com";

        final String documentCategory = DEFENDANT_LEVEL;

        final CpsDefendant defendant1 = CpsDefendant.cpsDefendant().withAsn(defendantId1.toString()).build();
        final CpsDefendant defendant2 = CpsDefendant.cpsDefendant().withAsn(defendantId2.toString()).build();

        final CpsMaterialSubmitted cpsMaterialSubmitted = CpsMaterialSubmitted.cpsMaterialSubmitted()
                .withCompassCaseId(compassCaseId)
                .withDefendants(Stream.of(defendant1, defendant2).collect(Collectors.toList()))
                .withDocuments(singletonList(CpsDocument.cpsDocument().withDocumentId(UUID.randomUUID().toString()).withFileStoreId(materialId).withMaterialType(documentType).build()))
                .withResponseEmail(responseEmail)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus.PENDING)
                .withTransactionID(transactionId)
                .withUrn(URN)
                .build();

        final UUID messageId = randomUUID();
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        final Envelope<CpsMaterialSubmitted> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.cps-material-submitted")
                        .withId(messageId)
                        .build(),
                cpsMaterialSubmitted
        );

        final UUID caseFileId = randomUUID();

        when(systemIdMapperService.getCppCaseIdFor(URN)).thenReturn(caseFileId);
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData().withTargetSectionCode("01").build());
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData().withDocumentCategory(documentCategory).build());

        processor.onCpsMaterialSubmitted(materialSubmittedEnvelope);

        verify(sender, times(2)).sendAsAdmin(envelope);

        captor.getAllValues().forEach(jsonEnvelope -> verifyPayloadForDefendantLevelDocumentCategory(defendantId1, defendantId2, materialId, caseFileId, jsonEnvelope));
    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialForEachDefendantWhenCpsMaterialSubmittedWithDefendantLevelDocAndPetFormDocument() {
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID materialId = randomUUID();
        final Integer compassCaseId = (int) Math.random();
        final Integer transactionId = (int) Math.random();
        final Integer documentType = 0;
        final String responseEmail = "responseEmail@mail.com";

        final String documentCategory = DEFENDANT_LEVEL;

        final CpsDefendant defendant1 = CpsDefendant.cpsDefendant().withAsn(defendantId1.toString()).build();
        final CpsDefendant defendant2 = CpsDefendant.cpsDefendant().withAsn(defendantId2.toString()).build();

        final CpsMaterialSubmitted cpsMaterialSubmitted = CpsMaterialSubmitted.cpsMaterialSubmitted()
                .withCompassCaseId(compassCaseId)
                .withDefendants(Stream.of(defendant1, defendant2).collect(Collectors.toList()))
                .withDocuments(singletonList(CpsDocument.cpsDocument().withDocumentId(UUID.randomUUID().toString()).withFileStoreId(materialId).withMaterialType(documentType).withFileName("Magistrates' Court PET FORM:Test Dcoument").build()))
                .withResponseEmail(responseEmail)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus.PENDING)
                .withTransactionID(transactionId)
                .withUrn(URN)
                .build();

        final UUID messageId = randomUUID();
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        final Envelope<CpsMaterialSubmitted> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.cps-material-submitted")
                        .withId(messageId)
                        .build(),
                cpsMaterialSubmitted
        );

        final UUID caseFileId = randomUUID();

        when(systemIdMapperService.getCppCaseIdFor(URN)).thenReturn(caseFileId);
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData().withTargetSectionCode("01").build());
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData().withDocumentCategory(documentCategory).build());

        processor.onCpsMaterialSubmitted(materialSubmittedEnvelope);

        verify(sender, times(2)).sendAsAdmin(envelope);

        captor.getAllValues().forEach(jsonEnvelope -> verifyPayloadForDefendantLevelDocumentCategoryWithPertFormDocument(defendantId1, defendantId2, materialId, caseFileId, jsonEnvelope));
    }

    private void verifyPayloadForCaseLevelDocumentCategory(final UUID defendantId1, final UUID defendantId2, final UUID materialId, final UUID caseFileId, final JsonEnvelope actualEnvelope) {
        verifyPayload(defendantId1, defendantId2, materialId, caseFileId, actualEnvelope);
        assertThat(actualEnvelope.payloadAsJsonObject().getJsonObject("prosecutorDefendantId"), nullValue());
    }

    private void verifyPayloadForDefendantLevelDocumentCategory(final UUID defendantId1, final UUID defendantId2, final UUID materialId, final UUID caseFileId, final JsonEnvelope actualEnvelope) {
        verifyPayload(defendantId1, defendantId2, materialId, caseFileId, actualEnvelope);
        assertThat(actualEnvelope.payloadAsJsonObject().getString("prosecutorDefendantId"), anyOf(is(defendantId1.toString()), is(defendantId2.toString())));
    }

    private void verifyPayloadForDefendantLevelDocumentCategoryWithPertFormDocument(final UUID defendantId1, final UUID defendantId2, final UUID materialId, final UUID caseFileId, final JsonEnvelope actualEnvelope) {
        verifyPayloadForDefendantLevelDocumentCategory(defendantId1, defendantId2, materialId, caseFileId, actualEnvelope);
        final JsonObject material = actualEnvelope.payloadAsJsonObject().getJsonObject("material");
        assertThat(material.getString("documentType"), is("Case Management"));

    }

    private void verifyPayload(final UUID defendantId1, final UUID defendantId2, final UUID materialId, final UUID caseFileId, final JsonEnvelope actualEnvelope) {
        final Metadata actualMetadata = actualEnvelope.metadata();
        assertThat(actualMetadata.name(), is("prosecutioncasefile.add-cps-material"));
        assertThat(actualMetadata.asJsonObject().getString("submissionId"), is(SUBMISSION_ID.toString()));
        final JsonObject payload = actualEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseFileId.toString()));
        final JsonObject material = payload.getJsonObject("material");
        assertThat(material.getString("fileStoreId"), is(materialId.toString()));
    }

    private void verifyPayloadV2(final UUID caseId, final MaterialSubmittedV3 materialSubmitted, final JsonEnvelope actualEnvelope) {
        assertThat(actualEnvelope.metadata().name(), is("prosecutioncasefile.add-material-v2"));
        assertThat(actualEnvelope.metadata().asJsonObject().getString("submissionId"), is(SUBMISSION_ID.toString()));

        final JsonObject payload = actualEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getString("submissionId"), is((materialSubmitted.getSubmissionId().toString())));
        assertThat(payload.getString("material"), is(materialSubmitted.getMaterial().toString()));
        assertThat(getBoolean(payload,"isCpsCase").orElse(null), is(materialSubmitted.getIsCpsCase()));

        final JsonObject prosecutionCaseSubject = payload.getJsonObject("prosecutionCaseSubject");
        assertThat(prosecutionCaseSubject.getString("caseUrn"), is(URN));
        assertThat(prosecutionCaseSubject.getString("prosecutingAuthority"), is(PROSECUTING_AUTHORITY));
        assertThat(getString(payload,"submissionStatus"), is(empty()));
        assertThat(getString(payload,"receivedDateTime"), is(not(empty())));

        final Optional<JsonObject> courtApplicationSubject = getJsonObject(payload, "courtApplicationSubject");
        assertThat(courtApplicationSubject, is(empty()));
    }


    private void verifyCourtApplicationPayloadV2(final MaterialSubmittedV3 materialSubmitted, final UUID courtApplicationId,  final JsonEnvelope actualEnvelope) {
        assertThat(actualEnvelope.metadata().name(), is("prosecutioncasefile.add-application-material-v2"));
        assertThat(actualEnvelope.metadata().asJsonObject().getString("submissionId"), is(SUBMISSION_ID.toString()));

        final JsonObject payload = actualEnvelope.payloadAsJsonObject();

        assertThat(payload.getString("submissionId"), is((materialSubmitted.getSubmissionId().toString())));
        assertThat(payload.getString("applicationId"), is(courtApplicationId.toString()));
        assertThat(payload.getString("material"), is(materialSubmitted.getMaterial().toString()));
        assertThat(getBoolean(payload,"isCpsCase"), is(empty()));
        assertThat(getString(payload,"receivedDateTime"), is(not(empty())));

        final JsonObject courtApplicationSubject = payload.getJsonObject("courtApplicationSubject");
        assertThat(courtApplicationSubject.getString("courtApplicationId"), is(courtApplicationId.toString()));
        assertThat(getString(payload,"submissionStatus"), is(empty()));

        final Optional<JsonObject> prosecutionCaseSubject = getJsonObject(payload, "prosecutionCaseSubject");
        assertThat(prosecutionCaseSubject, is(empty()));
    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialWhenMaterialSubmittedAndProsecutingAuthorityIsNull() {
        final String defendantId = randomUUID().toString();
        final String documentType = "SJPN";
        final UUID materialId = randomUUID();

        final MaterialSubmitted materialSubmitted = materialSubmitted()
                .withCaseUrn(URN)
                .withDefendantId(defendantId)
                .withMaterialType(documentType)
                .withProsecutingAuthority(null)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(PENDING)
                .withMaterialId(materialId)
                .withIsCpsCase(false)
                .build();

        final UUID messageId = randomUUID();
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        final Envelope<MaterialSubmitted> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.material-submitted")
                        .withId(messageId)
                        .build(),
                materialSubmitted
        );

        final UUID caseFileId = randomUUID();

        when(systemIdMapperService.getCppCaseIdFor(URN)).thenReturn(caseFileId);

        processor.onMaterialSubmitted(materialSubmittedEnvelope);

        verify(sender, times(1)).sendAsAdmin(envelope);

        final JsonEnvelope actualEnvelope = captor.getValue();

        final Metadata actualMetadata = actualEnvelope.metadata();
        assertThat(actualMetadata.name(), is("prosecutioncasefile.add-material"));
        assertThat(actualMetadata.asJsonObject().getString("submissionId"), is(SUBMISSION_ID.toString()));

        final JsonObject payload = actualEnvelope.payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseFileId.toString()));
        assertThat(payload.getString("prosecutorDefendantId"), is(defendantId));
        final JsonObject material = payload.getJsonObject("material");
        assertThat(material.getString("documentType"), is(documentType));
        assertThat(material.getString("fileStoreId"), is(materialId.toString()));

    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterial3WhenMaterialSubmittedWithProsecutionCaseSubject() {

        final UUID messageId = randomUUID();
        final UUID caseId = randomUUID();

        final MaterialSubmittedV3 materialSubmitted = createMaterialSubmittedWithProsecutionCaseSubject();

        final Envelope<MaterialSubmittedV3> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.material-submitted-v3")
                        .withId(messageId)
                        .build(),
                materialSubmitted
        );
        when(systemIdMapperService.getCaseIdForMaterialSubmission(PROSECUTING_AUTHORITY + ":" + URN)).thenReturn(caseId);
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        processor.materialSubmittedV3(materialSubmittedEnvelope);

        verify(sender).sendAsAdmin(any(JsonEnvelope.class));

        verifyPayloadV2(caseId, materialSubmitted, captor.getValue());
    }

    @Test
    public void shouldCallProsecutorCaseFileSubmitMaterialV3WhenMaterialSubmittedWithCourtApplicationSubject() {

        final UUID messageId = randomUUID();
        final UUID courtApplicationId = randomUUID();

        final MaterialSubmittedV3 materialSubmitted = createMaterialSubmittedWithCourtApplicationSubject(courtApplicationId);

        final Envelope<MaterialSubmittedV3> materialSubmittedEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName("stagingprosecutors.event.material-submitted-v3")
                        .withId(messageId)
                        .build(),
                materialSubmitted
        );
        final JsonEnvelope envelope = Mockito.mock(JsonEnvelope.class);

        final ArgumentCaptor<JsonEnvelope> captor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(envelopeHelper.withMetadataInPayload(captor.capture())).thenReturn(envelope);

        processor.materialSubmittedV3(materialSubmittedEnvelope);

        verify(sender).sendAsAdmin(any(JsonEnvelope.class));

        verifyCourtApplicationPayloadV2(materialSubmitted, courtApplicationId, captor.getValue());
    }

    private MaterialSubmittedV3 createMaterialSubmittedWithProsecutionCaseSubject() {
        return materialSubmittedV3()
                .withMaterialType(DOCUMENT_TYPE)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(SubmissionStatus.PENDING)
                .withProsecutionCaseSubject(createProsecutionCaseSubject())
                .withMaterial(randomUUID())
                .build();
    }

    private MaterialSubmittedV3 createMaterialSubmittedWithCourtApplicationSubject(final UUID courtApplicationId) {
        return materialSubmittedV3()
                .withMaterialType(DOCUMENT_TYPE)
                .withSubmissionId(SUBMISSION_ID)
                .withSubmissionStatus(SubmissionStatus.PENDING)
                .withCourtApplicationSubject(createCourtApplicationSubject(courtApplicationId))
                .withMaterial(randomUUID())
                .build();
    }

    private ProsecutionCaseSubject createProsecutionCaseSubject() {
        return new ProsecutionCaseSubject.Builder()
                .withCaseUrn(URN)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .build();
    }

    private CourtApplicationSubject createCourtApplicationSubject(final UUID courtApplicationId) {
        return new CourtApplicationSubject.Builder()
                .withCourtApplicationId(courtApplicationId)
                .build();
    }
}