package uk.gov.moj.cpp.staging.prosecutors.unbundling.file;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.PD_DOC_DEFENDANT_NAME;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.PRE_CON_PDF_NAME;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_BOOKMARKS_PDF_NAME;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileUploader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.PDFBundleDetails;
import uk.gov.moj.cpp.staging.prosecutors.test.util.DocumentType;
import uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"squid:S1607"})
@ExtendWith(MockitoExtension.class)
public class FileUploaderTest {

    @InjectMocks
    private FileUploader fileUploader;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<JsonObject> metadataCaptor;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamArgumentCaptor;
    private PDFTestHelper pdfTestHelper = new PDFTestHelper();

    @Test
    public void shouldUploadDocumentToFileStoreAndValidateReferenceIdWithoutDefendantName() throws IOException, FileServiceException {
        shouldUploadDocumentSuccessfully(SIMPLE_BOOKMARKS_PDF_NAME, DocumentType.SIMPLE_BOOKMARK_PDF, false);
    }

    @Test
    public void shouldUploadDocumentToFileStoreAndValidateReferenceIdWithDefendantName() throws IOException, FileServiceException {
        shouldUploadDocumentSuccessfully(PRE_CON_PDF_NAME, DocumentType.IDPC_CHARGES_AND_PRECON_ONLY, true);
    }

    @Test
    public void shouldThrowFileServerException() throws IOException, FileServiceException {
        when(fileService.store(any(JsonObject.class), any(InputStream.class))).thenThrow(FileServiceException.class);
        assertThrows(FileServiceException.class, () -> fileUploader.uploadFile(getPdDocumentHolder(SIMPLE_BOOKMARKS_PDF_NAME, DocumentType.SIMPLE_BOOKMARK_PDF), PD_DOC_DEFENDANT_NAME));
    }

    @Test
    public void shouldThrowIOException() {
        assertThrows(NullPointerException.class, () -> fileUploader.uploadFile(null, PD_DOC_DEFENDANT_NAME));
    }

    private void shouldUploadDocumentSuccessfully(String documentName, DocumentType documentType, boolean assertDefendantName) throws FileServiceException, IOException {
        final PDDocument pdDocument = pdfTestHelper.getPDDocument(documentType);
        when(fileService.store(any(JsonObject.class), any(InputStream.class))).thenReturn(UUID.randomUUID());
        final PDFBundleDetails pdfBundleDetails = new PDFBundleDetails();

        pdfBundleDetails.setBundleSections(singletonList(BundleSection.bundleSection().withBundleSectionName("IDPC_BOOKMARKS_1").build()));

        final FileHolder fileUploadReference = fileUploader.uploadFile(getPdDocumentHolder(documentName, documentType), PD_DOC_DEFENDANT_NAME);
        assertThat(fileUploadReference, notNullValue());
        assertThat("Charges", is(fileUploadReference.getSectionName()));
        assertInputStreamArgument(pdDocument);
        assertMetaDataArgument(assertDefendantName);
    }

    private void assertInputStreamArgument(PDDocument pdDocument) throws FileServiceException, IOException {
        verify(fileService).store(any(), inputStreamArgumentCaptor.capture());
        final InputStream inputStream = inputStreamArgumentCaptor.getValue();

        final PDDocument pdDocumentFromInStream = PDDocument.load(inputStream);
        assertThat(pdDocumentFromInStream.getPages().getCount(), is(pdDocument.getPages().getCount()));
        assertThat(pdDocumentFromInStream.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle(), is(pdDocument.getDocumentCatalog().getDocumentOutline().getFirstChild().getTitle()));
    }

    private void assertMetaDataArgument(boolean shouldContainDefendantName) throws FileServiceException {
        verify(fileService).store(metadataCaptor.capture(), any());
        final JsonObject metaData = metadataCaptor.getValue();
        if (shouldContainDefendantName) {
            assertThat("Filename in the metadata should include defendant name ", true, is(metaData.getString("fileName").contains(PD_DOC_DEFENDANT_NAME)));
        } else {
            assertThat("Filename in the metadata should not include defendant name ", false, is(metaData.getString("fileName").contains(PD_DOC_DEFENDANT_NAME)));
        }
    }

    private PDDocumentHolder getPdDocumentHolder(String documentName, DocumentType documentType) {
        return new PDDocumentHolder(documentName, pdfTestHelper.getPDDocument(documentType), "Charges");
    }
}