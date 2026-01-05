package uk.gov.moj.cpp.staging.prosecutors.unbundling;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.APPLICATION_PDF;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.CASE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FAILED_TO_RETRIEVE_FILE_SERVICE_EXCEPTION;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_STORE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATETIME_FORMATTER;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATE_TIME;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_BOOKMARKS_PDF_NAME;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundled;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundledV2;
import uk.gov.moj.cpp.staging.prosecutors.domain.Material;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.DocumentUnbundleEventProcessor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.DocumentNotFoundException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.InvalidPDFOutlineException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileSplitter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileUploader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.CmsDocumentIdentifier;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.DocumentBundleArrivedForUnbundling;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.utility.DocumentUnbundleResultBuilder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentUnbundleEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private FileService fileService;

    @InjectMocks
    private DocumentUnbundleEventProcessor documentUnbundleEventProcessor;

    @Mock
    private Envelope<DocumentBundleArrivedForUnbundling> bundleArrivedEnvelope;

    private UUID fileStoreId = UUID.randomUUID();

    @Mock
    private DocumentBundleArrivedForUnbundling documentBundleArrivedForUnbundling;

    @Mock
    private CmsDocumentIdentifier cmsDocumentIdentifier;

    @Mock
    FileUploader fileUploader;

    @Mock
    private Envelope<DocumentUnbundled> unBundleEnvelope;

    @Mock
    private Envelope<DocumentUnbundledV2> unBundleEnvelopeV2;

    @Mock
    private DocumentUnbundled documentUnbundled;

    @Mock
    private DocumentUnbundledV2 documentUnbundledV2;

    @Mock
    private FileSplitter fileSplitter;

    @Mock
    private DocumentUnbundleResultBuilder documentUnbundleResultBuilder;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private ExecutionService executionService;

    @Mock
    private UtcClock clock;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<ExecutionInfo> executionInfoArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<FileHolder> fileHolderArgumentCaptor;

    private final UUID caseId = UUID.randomUUID();
    private final String prosecutorDefendantId = "TVLA1234";
    private final String randomProsecutionAuthority = "RANDOM_PROSECUTION_AUTHORITY";
    private final String defendantName = "RAND_DEFENDANT_NAME";
    private final ZonedDateTime receivedDateTime = ZonedDateTime.of(2018, 01, 01, 10, 12, 33, 0, ZoneId.of("UTC"));
    private static FileReference fileReference;

    @BeforeEach
    public void setUp() throws IOException {
        documentUnbundleEventProcessor.setChunkCount("50");
        fileReference = getFileReference();
    }

    @Test
    public void shouldRecordDocumentUnBundleResultWithErrorWhenFailToRetrieveDocumentBundle() throws Exception {
        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(fileService.retrieve(fileStoreId)).thenThrow(FileServiceException.class);
        when(documentUnbundleResultBuilder.getFailedResult(any(DocumentBundleArrivedForUnbundling.class), any(String.class))).thenReturn(jsonEnvelope);

        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);

        verify(documentUnbundleResultBuilder).getFailedResult(any(DocumentBundleArrivedForUnbundling.class), eq(FAILED_TO_RETRIEVE_FILE_SERVICE_EXCEPTION));
        verify(sender).sendAsAdmin(any(Envelope.class));
    }

    @Test
    public void shouldRecordDocumentUnBundleResultWithErrorWhenDocumentNotFound() throws FileServiceException {
        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(fileService.retrieve(fileStoreId)).thenThrow(DocumentNotFoundException.class);
        when(documentUnbundleResultBuilder.getFailedResult(any(DocumentBundleArrivedForUnbundling.class), any(String.class))).thenReturn(jsonEnvelope);

        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);

        verify(documentUnbundleResultBuilder).getFailedResult(any(DocumentBundleArrivedForUnbundling.class), eq(DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID + fileStoreId));
        verify(sender).sendAsAdmin(any(Envelope.class));
    }

    @Test
    public void shouldRecordDocumentUnBundleResultWithErrorWhenFailToSplitDocument() throws FileServiceException,IOException {
        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(cmsDocumentIdentifier.getMaterialType()).thenReturn(1);
        when(fileSplitter.split(any(PDDocument.class), anyInt())).thenAnswer(i -> {throw new IOException("custom test exception");});

        when(fileService.retrieve(fileStoreId)).thenReturn(Optional.of(fileReference));
        when(documentUnbundleResultBuilder.getFailedResult(any(DocumentBundleArrivedForUnbundling.class), any(String.class))).thenReturn(jsonEnvelope);

        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);

        verify(documentUnbundleResultBuilder).getFailedResult(any(DocumentBundleArrivedForUnbundling.class), eq(ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE));
        verify(sender).sendAsAdmin(any(Envelope.class));

    }

    @Test
    public void shouldRecordDocumentUnBundleResultWithErrorWhenBookmarksAreInvalid() throws FileServiceException {
        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(cmsDocumentIdentifier.getMaterialType()).thenReturn(1);

        when(fileSplitter.split(any(PDDocument.class), anyInt())).thenThrow(InvalidPDFOutlineException.class);
        when(fileService.retrieve(fileStoreId)).thenReturn(Optional.of(fileReference));
        when(documentUnbundleResultBuilder.getFailedResult(any(DocumentBundleArrivedForUnbundling.class), any(String.class))).thenReturn(jsonEnvelope);

        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);

        verify(documentUnbundleResultBuilder).getFailedResult(any(DocumentBundleArrivedForUnbundling.class), eq(DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS));
        verify(sender).sendAsAdmin(any(Envelope.class));
    }

    @Test
    public void shouldUnBundleDocumentsSuccessfully() throws FileServiceException, IOException {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final List<FileHolder> fileHolderList = asList(new FileHolder(randomUUID(), "Case summary"),
                new FileHolder(randomUUID(), "Charges"),
                new FileHolder(randomUUID(), "Key Exhibits"));

        final List<PDDocumentHolder> pdfDocumentHolderList = asList(new PDDocumentHolder("doc1", new PDDocument(), "sec1"),
                new PDDocumentHolder("doc2", new PDDocument(), "sec2"), new PDDocumentHolder("doc3", new PDDocument(), "sec3"));
        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(documentBundleArrivedForUnbundling.getMaterial()).thenReturn(getMaterial(Optional.empty()));
        when(documentBundleArrivedForUnbundling.getCmsDocumentIdentifier()).thenReturn(cmsDocumentIdentifier);
        when(cmsDocumentIdentifier.getMaterialType()).thenReturn(1);

        when(bundleArrivedEnvelope.payload()).thenReturn(documentBundleArrivedForUnbundling);
        when(fileService.retrieve(fileStoreId)).thenReturn(Optional.of(fileReference));
        when(fileSplitter.split(any(PDDocument.class), anyInt())).thenReturn(pdfDocumentHolderList);
        when(fileUploader.uploadFile(any(PDDocumentHolder.class), any())).thenReturn(fileHolderList.get(0), fileHolderList.get(1), fileHolderList.get(2));
        when(documentUnbundleResultBuilder.buildPayloadForFile(fileHolderList.get(0))).thenReturn(createObjectBuilder()
                .add(DOCUMENT_TYPE, fileHolderList.get(0).getSectionName())
                .add(FILE_STORE_ID, fileHolderList.get(0).getFileStoreId().toString())
                .add(FILE_TYPE, APPLICATION_PDF)
                .build());
        when(documentUnbundleResultBuilder.buildPayloadForFile(fileHolderList.get(1))).thenReturn(createObjectBuilder()
                .add(DOCUMENT_TYPE, fileHolderList.get(1).getSectionName())
                .add(FILE_STORE_ID, fileHolderList.get(1).getFileStoreId().toString())
                .add(FILE_TYPE, APPLICATION_PDF)
                .build());
        when(documentUnbundleResultBuilder.buildPayloadForFile(fileHolderList.get(2))).thenReturn(createObjectBuilder()
                .add(DOCUMENT_TYPE, fileHolderList.get(2).getSectionName())
                .add(FILE_STORE_ID, fileHolderList.get(2).getFileStoreId().toString())
                .add(FILE_TYPE, APPLICATION_PDF)
                .build());

        when(documentUnbundleResultBuilder.initializePayload(documentBundleArrivedForUnbundling)).thenReturn(createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(PROSECUTOR_DEFENDANT_ID, defendantId.toString())
                .add(RECEIVED_DATE_TIME,ZonedDateTime.now().format(RECEIVED_DATETIME_FORMATTER)));


        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);


        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope envelope = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(envelope.metadata().name(), is("stagingprosecutors.command.record-unbundled-document-results"));
        assertThat(((JsonObject)envelope.payload()).getString("caseId"), is(caseId.toString()));
        assertThat(((JsonObject)envelope.payload()).getString("prosecutorDefendantId"), is(defendantId.toString()));

        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").size(), is(3));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(0).getString(FILE_STORE_ID), is(fileHolderList.get(0).getFileStoreId().toString()));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(0).getString(DOCUMENT_TYPE), is(fileHolderList.get(0).getSectionName()));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(1).getString(FILE_STORE_ID), is(fileHolderList.get(1).getFileStoreId().toString()));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(1).getString(DOCUMENT_TYPE), is(fileHolderList.get(1).getSectionName()));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(2).getString(FILE_STORE_ID), is(fileHolderList.get(2).getFileStoreId().toString()));
        assertThat(((JsonObject)envelope.payload()).getJsonArray("materials").getJsonObject(2).getString(DOCUMENT_TYPE), is(fileHolderList.get(2).getSectionName()));


    }

    @Test
    public void givenUnBundleDocumentsWhenBundleUploadFailedShouldSendFailedMessage() throws FileServiceException, IOException {

        final List<FileHolder> fileHolderList = asList(new FileHolder(randomUUID(), "Case summary"),
                new FileHolder(randomUUID(), "Charges"),
                new FileHolder(randomUUID(), "Key Exhibits"));

        final List<PDDocumentHolder> pdfDocumentHolderList = asList(new PDDocumentHolder("doc1", new PDDocument(), "sec1"),
                new PDDocumentHolder("doc2", new PDDocument(), "sec2"), new PDDocumentHolder("doc3", new PDDocument(), "sec3"));

        when(bundleArrivedEnvelope.payload()).thenReturn(getDocumentBundleArrivedForUnBundlingSample(getMaterial(Optional.empty())));
        when(documentBundleArrivedForUnbundling.getMaterial()).thenReturn(getMaterial(Optional.empty()));
        when(documentBundleArrivedForUnbundling.getCmsDocumentIdentifier()).thenReturn(cmsDocumentIdentifier);
        when(cmsDocumentIdentifier.getMaterialType()).thenReturn(1);

        when(bundleArrivedEnvelope.payload()).thenReturn(documentBundleArrivedForUnbundling);
        when(fileService.retrieve(fileStoreId)).thenReturn(Optional.of(fileReference));
        when(fileSplitter.split(any(PDDocument.class), anyInt())).thenReturn(pdfDocumentHolderList);
        when(fileUploader.uploadFile(any(PDDocumentHolder.class), any())).thenThrow(new FileServiceException("Service down!"));
        when(documentUnbundleResultBuilder.getFailedResult(any(DocumentBundleArrivedForUnbundling.class), any(String.class))).thenReturn(jsonEnvelope);

        documentUnbundleEventProcessor.handleDocumentUnbundling(bundleArrivedEnvelope);

        verify(documentUnbundleResultBuilder).getFailedResult(documentBundleArrivedForUnbundling, "Failed to retrieve bundle FileService has throw an exception");
        verify(sender).sendAsAdmin(any(Envelope.class));
    }

    @Test
    public void shouldSendAddCPSMaterialRequestToPcfWhenDocumentUnbundledEventArrivesWithoutMaterialTypeAndDefendentUUID() {
        when(documentUnbundled.getMaterial()).thenReturn(getMaterial(Optional.empty()));
        when(envelopeHelper.withMetadataInPayload(any(JsonEnvelope.class))).thenReturn(jsonEnvelope);

        assertHandleDocumentUnbundled(getMaterial(Optional.empty()), Optional.empty());
    }

    @Test
    public void shouldSendAddCPSMaterialRequestToPcfWhenDocumentUnbundledEventArrivesWithMaterialTypeAndDefendentUUID() {
        when(documentUnbundled.getMaterial()).thenReturn(getMaterial(Optional.empty()));
        when(envelopeHelper.withMetadataInPayload(any(JsonEnvelope.class))).thenReturn(jsonEnvelope);

        assertHandleDocumentUnbundled(getMaterial(Optional.of("materialType")), Optional.of(prosecutorDefendantId));
    }

    @Test
    public void shouldSendAddCPSMaterialRequestToPcfWhenDocumentUnbundledEventArrivesWithMultipleMaterialsAndDefendentUUID() {
        when(envelopeHelper.withMetadataInPayload(any(JsonEnvelope.class))).thenReturn(jsonEnvelope);

        assertHandleDocumentUnbundledV2(asList(getMaterial(Optional.of("docType1")), getMaterial(Optional.of("docType2"))), Optional.of(prosecutorDefendantId));
    }

    @Test
    public void shouldSendAddCPSMaterialRequestToPcfInChunksWhenDocumentUnbundledEventArrivesWithMultipleMaterialsAndDefendentUUID() {

        when(unBundleEnvelopeV2.payload()).thenReturn(documentUnbundledV2);
        when(unBundleEnvelopeV2.metadata()).thenReturn(getMetadata());
        when(documentUnbundledV2.getCaseId()).thenReturn(caseId);
        when(documentUnbundledV2.getMaterials()).thenReturn(IntStream.range(1, 60).mapToObj(i -> getMaterial(Optional.of("docType1"))).toList());
        when(documentUnbundledV2.getProsecutingAuthority()).thenReturn(Optional.of("DVL"));
        when(documentUnbundledV2.getProsecutorDefendantId()).thenReturn(prosecutorDefendantId);
        when(documentUnbundledV2.getReceivedDateTime()).thenReturn(receivedDateTime);
        when(clock.now()).thenReturn(ZonedDateTime.now());


        documentUnbundleEventProcessor.handleDocumentUnbundledV2(unBundleEnvelopeV2);

        verify(executionService, times(2)).executeWith(executionInfoArgumentCaptor.capture());
        assertThat(executionInfoArgumentCaptor.getAllValues().get(0).getJobData().getJsonObject("payload").getJsonArray("materials").size(), is(50));
        assertThat(executionInfoArgumentCaptor.getAllValues().get(1).getJobData().getJsonObject("payload").getJsonArray("materials").size(), is(9));
        assertThat(ChronoUnit.MILLIS.between( executionInfoArgumentCaptor.getAllValues().get(0).getNextTaskStartTime(), executionInfoArgumentCaptor.getAllValues().get(1).getNextTaskStartTime()), greaterThan(Long.parseLong("900")) );

    }

    public void assertHandleDocumentUnbundled(Material material, Optional<String> defendantId) {

        when(unBundleEnvelope.payload()).thenReturn(documentUnbundled);
        when(unBundleEnvelope.metadata()).thenReturn(getMetadata());
        when(documentUnbundled.getCaseId()).thenReturn(caseId);
        when(documentUnbundled.getMaterial()).thenReturn(material);
        when(documentUnbundled.getProsecutingAuthority()).thenReturn(Optional.of("DVL"));
        when(documentUnbundled.getMaterial()).thenReturn(material);
        when(documentUnbundled.getProsecutorDefendantId()).thenReturn(prosecutorDefendantId);
        when(documentUnbundled.getReceivedDateTime()).thenReturn(receivedDateTime);

        documentUnbundleEventProcessor.handleDocumentUnbundled(unBundleEnvelope);

        verify(envelopeHelper).withMetadataInPayload(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(any(Envelope.class));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject(), notNullValue());
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutorDefendantId"), is(prosecutorDefendantId));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutingAuthority"), is("DVL"));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("receivedDateTime"), is(receivedDateTime.format(RECEIVED_DATETIME_FORMATTER)));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("material").getString("fileStoreId"), is(getMaterial(Optional.empty()).getFileStoreId().toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("material").getString("fileType"), is("application/pdf"));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("material").getString("documentType"), is(material.getDocumentType()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("material").getBoolean("isUnbundledDocument"), is(true));
        defendantId.ifPresent(id -> assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutorDefendantId"), is(defendantId.get())));
    }

    public void assertHandleDocumentUnbundledV2(List<Material> materials, Optional<String> defendantId) {
        when(unBundleEnvelopeV2.payload()).thenReturn(documentUnbundledV2);
        when(unBundleEnvelopeV2.metadata()).thenReturn(getMetadata());
        when(documentUnbundledV2.getCaseId()).thenReturn(caseId);
        when(documentUnbundledV2.getMaterials()).thenReturn(materials);
        when(documentUnbundledV2.getProsecutingAuthority()).thenReturn(Optional.of("DVL"));
        when(documentUnbundledV2.getMaterials()).thenReturn(materials);
        when(documentUnbundledV2.getProsecutorDefendantId()).thenReturn(prosecutorDefendantId);
        when(documentUnbundledV2.getReceivedDateTime()).thenReturn(receivedDateTime);

        documentUnbundleEventProcessor.handleDocumentUnbundledV2(unBundleEnvelopeV2);

        verify(envelopeHelper).withMetadataInPayload(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(any(Envelope.class));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject(), notNullValue());
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutorDefendantId"), is(prosecutorDefendantId));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutingAuthority"), is("DVL"));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("receivedDateTime"), is(receivedDateTime.format(RECEIVED_DATETIME_FORMATTER)));
        final JsonArray actualMaterials = jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonArray("materials");

        for (Material expectedMaterial : materials) {
            final List<JsonObject> actualMatchingMaterials = actualMaterials.getValuesAs(JsonObject.class).stream().filter(jsonObject -> jsonObject.getString("documentType").equals(expectedMaterial.getDocumentType())).collect(Collectors.toList());
            assertThat(actualMatchingMaterials.size(), is(1));
            final JsonObject actualMaterial = actualMatchingMaterials.get(0);
            assertThat(actualMaterial.getString("fileStoreId"), is(expectedMaterial.getFileStoreId().toString()));
            assertThat(actualMaterial.getString("fileType"), is(expectedMaterial.getFileType()));
            assertThat(actualMaterial.getString("documentType"), is(expectedMaterial.getDocumentType()));
            assertThat(actualMaterial.getBoolean("isUnbundledDocument"), is(expectedMaterial.getIsUnbundledDocument()));
        }
        defendantId.ifPresent(id -> assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutorDefendantId"), is(defendantId.get())));
    }

    private Metadata getMetadata() {
        return metadataBuilder().withName("stagingprosecutors.event.document-unbundled").withId(UUID.randomUUID()).build();
    }

    private FileReference getFileReference() throws IOException {

        PDDocument pdDocument = new PDDocument();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        pdDocument.save(outStream);
        InputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());

        final String formatDate = DateUtils.formatDate(new Date());
        final JsonObject metaData = createObjectBuilder().add("fileName", SIMPLE_BOOKMARKS_PDF_NAME + "_" + randomUUID() + "_" + formatDate).build();
        return new FileReference(UUID.randomUUID(), metaData, inputStream);
    }

    private DocumentBundleArrivedForUnbundling getDocumentBundleArrivedForUnBundlingSample(uk.gov.moj.cpp.staging.prosecutors.domain.Material material) {
        return DocumentBundleArrivedForUnbundling.documentBundleArrivedForUnbundling().withCaseId(caseId).withProsecutorDefendantId(prosecutorDefendantId).withDefendantName(defendantName).withProsecutingAuthority(randomProsecutionAuthority).withMaterial(material).withCmsDocumentIdentifier(cmsDocumentIdentifier).withReceivedDateTime(receivedDateTime).build();
    }

    private Material getMaterial(Optional<String> documentType) {
        return Material.material().withDocumentType(documentType.orElse("")).withFileType(APPLICATION_PDF).withFileStoreId(fileStoreId).withIsUnbundledDocument(true).build();
    }
}
