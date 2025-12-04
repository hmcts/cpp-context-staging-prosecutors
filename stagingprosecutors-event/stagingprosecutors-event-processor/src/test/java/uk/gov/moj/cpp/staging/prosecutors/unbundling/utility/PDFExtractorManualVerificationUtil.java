package uk.gov.moj.cpp.staging.prosecutors.unbundling.utility;

import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.MAGISTRATE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.fetchMockReferenceData;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFExtractor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PDFExtractorManualVerificationUtil {

    private final Map<String, List<BundleSection>> bookmarksByFileType;

    public PDFExtractorManualVerificationUtil(Map<String, List<BundleSection>> bookmarksByFileType) {
        this.bookmarksByFileType = bookmarksByFileType;
    }

    private static final String TEMP_PDF_FILE_DIR = "stagingprosecutors-event/stagingprosecutors-event-processor/src/test/resources/upload_samples";
    private static final String TEMP_PDF_FILE_NAME = "SamplePDFFile.pdf";
    private static final String UPLOAD_OUTPUT_DIR = "output";
    private final PDFExtractor pdfExtractor = new PDFExtractor();

    private void splitIDPCDocumentForManualVerification() throws IOException {
        splitPDFAndSaveLocally(getFileAndCleanOutputDirectory(), bookmarksByFileType.get(IDPC_FILE_TYPE));
    }

    private void splitMagistrateDocumentForManualVerification() throws IOException {
        splitPDFAndSaveLocally(getFileAndCleanOutputDirectory(), bookmarksByFileType.get(MAGISTRATE_FILE_TYPE));
    }

    public File getFileAndCleanOutputDirectory() throws IOException {
        final String tempFileAbsolutePath = getAbsolutePath();
        final File tempPDFFile = new File(tempFileAbsolutePath + File.separator + TEMP_PDF_FILE_NAME);
        FileUtils.cleanDirectory(new File(tempFileAbsolutePath + File.separator + UPLOAD_OUTPUT_DIR));
        return tempPDFFile;
    }

    private String getAbsolutePath() {
        return Paths.get(TEMP_PDF_FILE_DIR).toFile().getAbsolutePath();
    }

    private void splitPDFAndSaveLocally(final File samplePDFFile, final List<BundleSection> expectedParentSections) throws IOException {
        final PDDocument pdDocument = PDDocument.load(samplePDFFile);
        final List<PDDocumentHolder> unbundledPDDocs = pdfExtractor.splitIntoSections(pdDocument, expectedParentSections);
        final String path = getAbsolutePath() + File.separator + UPLOAD_OUTPUT_DIR + File.separator;
        unbundledPDDocs.forEach(unbundledDoc -> {
            try {
                unbundledDoc.saveDocument(
                        path + "FolderName[ " + unbundledDoc.getSectionName() + " ]" +
                                " - FileName[ " + unbundledDoc.getDocumentName() + " ].pdf");
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    public static void main(String[] args) throws IOException {
        final PDFExtractorManualVerificationUtil extractorUtil = new PDFExtractorManualVerificationUtil(fetchMockReferenceData());
        extractorUtil.splitIDPCDocumentForManualVerification();
        extractorUtil.splitMagistrateDocumentForManualVerification();
    }

}
