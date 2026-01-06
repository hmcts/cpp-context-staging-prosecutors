package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.jobstore.tasks.TaskNames.CASE_FILE_ADD_MATERIAL_TASK;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.CASE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FAILED_TO_PROCESS_DOCUMENT_BUNDLE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FAILED_TO_RETRIEVE_BUNDLE_FILE_SERVICE_HAS_THROW_AN_EXCEPTION;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_STORE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.GENERIC_INVALID_PDF_EXCEPTION_MSG;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.IS_UNBUNDLED_DOCUMENT;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.MATERIAL;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.MATERIALS;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTION_CASE_FILE_ADD_MATERIAL;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTION_CASE_FILE_ADD_MATERIALS;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATETIME_FORMATTER;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECEIVED_DATE_TIME;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.RECORD_UNBUNDLE_DOCUMENT_RESULTS;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Priority;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundled;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundledV2;
import uk.gov.moj.cpp.staging.prosecutors.domain.Material;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.DocumentNotFoundException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.InvalidPDFOutlineException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.UnBundlingTechnicalException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileSplitter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileUploader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.DocumentBundleArrivedForUnbundling;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.utility.DocumentUnbundleResultBuilder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections4.ListUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class DocumentUnbundleEventProcessor {
    @Inject
    private Sender sender;

    @Inject
    private FileService fileService;

    @Inject
    private FileUploader fileUploader;

    @Inject
    private FileSplitter fileSplitter;

    @Inject
    private DocumentUnbundleResultBuilder documentUnbundleResultBuilder;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    @Value(key = "stagingprosecutors.submit-materials.chunkCount", defaultValue = "50")
    private String chunkCount;

    @Inject
    private ExecutionService executionService;

    @Inject
    private UtcClock clock;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUnbundleEventProcessor.class);

    @Handles("public.prosecutioncasefile.document-bundle-arrived-for-unbundling")
    public void handleDocumentUnbundling(final Envelope<DocumentBundleArrivedForUnbundling> envelope) {
        LOGGER.info("Received document bundle for unbundling with caseId : {}", envelope.payload().getCaseId());

        final DocumentBundleArrivedForUnbundling documentBundleArrivedForUnbundling = envelope.payload();
        final UUID bundleFileStoreId = documentBundleArrivedForUnbundling.getMaterial().getFileStoreId();

        String errorMessage = null;

        try {
            final Optional<FileReference> documentBundle = fileService.retrieve(bundleFileStoreId);
            if (documentBundle.isPresent()) {
                final Integer materialType = documentBundleArrivedForUnbundling.getCmsDocumentIdentifier().getMaterialType();

                try (final InputStream contentStream = documentBundle.get().getContentStream()) {
                    doUnbundling(documentBundleArrivedForUnbundling, materialType, contentStream);
                }
            } else {
                throw new DocumentNotFoundException(DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID + bundleFileStoreId);
            }
        } catch (InvalidPDFOutlineException ex) {
            errorMessage = DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS;
            LOGGER.error(DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS, ex);
        }
        catch (UnBundlingTechnicalException ex) {
            errorMessage = GENERIC_INVALID_PDF_EXCEPTION_MSG;
            LOGGER.error(GENERIC_INVALID_PDF_EXCEPTION_MSG, ex);
        }
        catch (FileServiceException ex) {
            errorMessage = FAILED_TO_RETRIEVE_BUNDLE_FILE_SERVICE_HAS_THROW_AN_EXCEPTION;
            LOGGER.error(FAILED_TO_RETRIEVE_BUNDLE_FILE_SERVICE_HAS_THROW_AN_EXCEPTION, ex);
        } catch (IOException ex) {
            errorMessage = ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE;
            LOGGER.error(ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE, ex);
        } catch (DocumentNotFoundException ex) {
            errorMessage = DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID + bundleFileStoreId;
            LOGGER.error(errorMessage, ex);
        }

        if (nonNull(errorMessage)) {
            handleUnbundlingException(documentBundleArrivedForUnbundling, errorMessage);
        }
    }

    @Handles("stagingprosecutors.event.document-unbundled")
    public void handleDocumentUnbundled(final Envelope<DocumentUnbundled> documentUnbundledEnvelope) {

        final DocumentUnbundled documentUnbundled = documentUnbundledEnvelope.payload();

        final JsonObjectBuilder materialJsonObjectBuilder = createObjectBuilder()
                .add(FILE_STORE_ID, documentUnbundled.getMaterial().getFileStoreId().toString())
                .add(FILE_TYPE, documentUnbundled.getMaterial().getFileType())
                .add(DOCUMENT_TYPE, documentUnbundled.getMaterial().getDocumentType())
                .add(IS_UNBUNDLED_DOCUMENT, documentUnbundled.getMaterial().getIsUnbundledDocument());

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, documentUnbundled.getCaseId().toString())
                .add(PROSECUTOR_DEFENDANT_ID, documentUnbundled.getProsecutorDefendantId())
                .add(RECEIVED_DATE_TIME, documentUnbundled.getReceivedDateTime().format(RECEIVED_DATETIME_FORMATTER))
                .add(MATERIAL, materialJsonObjectBuilder.build());

        documentUnbundled.getProsecutingAuthority().ifPresent(pa -> payloadBuilder.add(PROSECUTING_AUTHORITY, pa));

        final Metadata metadata = metadataFrom(documentUnbundledEnvelope.metadata())
                .withName(PROSECUTION_CASE_FILE_ADD_MATERIAL)
                .build();

        final Metadata jsonObject = metadataFrom(JsonObjects.createObjectBuilder(metadata.asJsonObject()).add("id", UUID.randomUUID().toString()).build()).build();
        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(jsonObject, payloadBuilder.build()));

        sender.sendAsAdmin(Envelope.envelopeFrom(jsonEnvelope.metadata(), jsonEnvelope.payload()));
    }


    // call task
    @Handles("stagingprosecutors.event.document-unbundled-v2")
    public void handleDocumentUnbundledV2(final Envelope<DocumentUnbundledV2> documentUnbundledEnvelope) {

        final DocumentUnbundledV2 documentUnbundled = documentUnbundledEnvelope.payload();
        final int chuckCountInteger = Integer.parseInt(chunkCount);

        final Metadata metadata = metadataFrom(documentUnbundledEnvelope.metadata())
                .withName(PROSECUTION_CASE_FILE_ADD_MATERIALS)
                .build();

        if(documentUnbundled.getMaterials().size() <= chuckCountInteger){
            final JsonObjectBuilder payloadBuilder = preparePayload(documentUnbundled.getMaterials(), documentUnbundled);
            final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(metadata, payloadBuilder.build()));

            sender.sendAsAdmin(Envelope.envelopeFrom(jsonEnvelope.metadata(), jsonEnvelope.payload()));

        } else {

            final AtomicReference<ZonedDateTime> runTime = new AtomicReference<>(clock.now());
            ListUtils.partition(documentUnbundled.getMaterials(), chuckCountInteger)
                    .forEach(materials -> {
                        sendMaterials(documentUnbundledEnvelope, materials, documentUnbundled, runTime, metadata);
                    });
        }

    }

    private void sendMaterials(final Envelope<DocumentUnbundledV2> documentUnbundledEnvelope, final List<Material> materials, final DocumentUnbundledV2 documentUnbundled, final AtomicReference<ZonedDateTime> runTime, final Metadata metadata) {
        final JsonObjectBuilder payloadBuilder = preparePayload(materials, documentUnbundled);


        final ExecutionInfo executionInfo = new ExecutionInfo(
                createObjectBuilder().add("metadata", metadata.asJsonObject()).add("payload", payloadBuilder.build()).build(),
                CASE_FILE_ADD_MATERIAL_TASK,
                runTime.get(),
                STARTED,
                true,
                Priority.HIGH);
        runTime.set(clock.now().plusSeconds(1));

        executionService.executeWith(executionInfo);
    }

    private static JsonObjectBuilder preparePayload(final List<Material> materials, final DocumentUnbundledV2 documentUnbundled) {
        final JsonArrayBuilder materialJsonArrayBuilder = createArrayBuilder();
        materials.stream()
                .map(material -> createObjectBuilder()
                        .add(FILE_STORE_ID, material.getFileStoreId().toString())
                        .add(FILE_TYPE, material.getFileType())
                        .add(DOCUMENT_TYPE, material.getDocumentType())
                        .add(IS_UNBUNDLED_DOCUMENT, material.getIsUnbundledDocument()))
                .forEach(materialJsonArrayBuilder::add);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, documentUnbundled.getCaseId().toString())
                .add(PROSECUTOR_DEFENDANT_ID, documentUnbundled.getProsecutorDefendantId())
                .add(RECEIVED_DATE_TIME, documentUnbundled.getReceivedDateTime().format(RECEIVED_DATETIME_FORMATTER))
                .add(MATERIALS, materialJsonArrayBuilder.build());

        documentUnbundled.getProsecutingAuthority().ifPresent(pa -> payloadBuilder.add(PROSECUTING_AUTHORITY, pa));
        return payloadBuilder;
    }

    private void doUnbundling(final DocumentBundleArrivedForUnbundling unbundlingObject,
                              final Integer materialType,
                              final InputStream contentStream) throws IOException, FileServiceException {
        try (final PDDocument pdDocument = PDDocument.load(contentStream)) {
            final List<PDDocumentHolder> pdDocumentHolders = fileSplitter.split(pdDocument, materialType);
            final JsonObjectBuilder unBundleDocumentResultsBuilder = documentUnbundleResultBuilder.initializePayload(unbundlingObject);
            final JsonArrayBuilder materialsArrayBuilder = JsonObjects.createArrayBuilder();

            for (final PDDocumentHolder pdDocumentHolder : pdDocumentHolders) {
                final FileHolder fileHolder = fileUploader.uploadFile(pdDocumentHolder, unbundlingObject.getDefendantName());
                final JsonObject materialObjectForFile = documentUnbundleResultBuilder.buildPayloadForFile(fileHolder);
                materialsArrayBuilder.add(materialObjectForFile);
            }

            unBundleDocumentResultsBuilder.add(MATERIALS ,materialsArrayBuilder);
            final JsonObject unBundleDocumentResultsCommandPayload =unBundleDocumentResultsBuilder.build();
            final Metadata metadata = metadataBuilder()
                    .withName(RECORD_UNBUNDLE_DOCUMENT_RESULTS)
                    .withId(UUID.randomUUID())
                    .build();
            final Metadata metadataJsonObject = metadataFrom(JsonObjects.createObjectBuilder(metadata.asJsonObject()).build()).build();
            sender.sendAsAdmin(Envelope.envelopeFrom(metadataJsonObject, unBundleDocumentResultsCommandPayload));

        }
    }

    private void handleUnbundlingException(final DocumentBundleArrivedForUnbundling documentBundleArrivedForUnbundling, final String errorMessage) {
        LOGGER.info(FAILED_TO_PROCESS_DOCUMENT_BUNDLE, errorMessage);
        final JsonEnvelope failedResultJson = documentUnbundleResultBuilder.getFailedResult(documentBundleArrivedForUnbundling, errorMessage);
        sender.sendAsAdmin(Envelope.envelopeFrom(failedResultJson.metadata(), failedResultJson.payload()));
    }

    public void setChunkCount(final String chunkCount) {
        this.chunkCount = chunkCount;
    }
}