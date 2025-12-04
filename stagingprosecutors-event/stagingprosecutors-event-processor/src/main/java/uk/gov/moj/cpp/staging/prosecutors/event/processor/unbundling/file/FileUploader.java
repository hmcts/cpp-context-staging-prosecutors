package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.ACCEPTED_CHAR;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.APPLICATION_PDF;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.FILE_NAME;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.INVALID_CHARS_IN_FILENAME;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.MEDIA_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.getDefendantNameExceptionList;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("squid:S1160")
public class FileUploader {
    @Inject
    private FileService fileService;

    public FileHolder uploadFile(final PDDocumentHolder pdDocumentHolder, final String defendantName) throws IOException, FileServiceException {
        Objects.requireNonNull(pdDocumentHolder);

        final UUID fileUploadReference = storeDocumentSection(pdDocumentHolder, defendantName);
        final FileHolder fileHolder = new FileHolder(fileUploadReference, pdDocumentHolder.getSectionName());
        pdDocumentHolder.close();

        return fileHolder;
    }

    private UUID storeDocumentSection(final PDDocumentHolder pdDocumentHolder, final String defendantName) throws FileServiceException, IOException {
        final String documentName = pdDocumentHolder.getDocumentName();
        final String fileName = getDefendantNameExceptionList().contains(documentName)
                ? String.format("%s_%s", defendantName, documentName)
                : documentName;

        final JsonObject metaData = createObjectBuilder()
                .add(FILE_NAME, getValidFilename(fileName))
                .add(MEDIA_TYPE, APPLICATION_PDF)
                .build();

        try (final ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            pdDocumentHolder.saveDocument(outStream);
            try (final InputStream inputStream = new ByteArrayInputStream(outStream.toByteArray())) {
                return fileService.store(metaData, inputStream);
            }
        }
    }

    private String getValidFilename(final String pdDocName) {
        return StringUtils.isNotEmpty(pdDocName)
                ? pdDocName.replaceAll(INVALID_CHARS_IN_FILENAME, ACCEPTED_CHAR)
                : pdDocName;
    }
}
