package uk.gov.moj.cpp.staging.prosecutors.unbundling.pdf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_SPLIT_ALL_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.MAGISTRATE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.fetchMockReferenceData;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFExtractor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.test.util.DocumentType;
import uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), SP-M05.
 *
 * The J25 upgrade moves PDFBox from 2.0.24 to 2.0.36 and adds a reachabilityFence to PDFExtractor because the JDK 25
 * GC could reclaim the source document mid-extraction. Follow the production path in DocumentUnbundleEventProcessor
 * (load the bundle from a stream, split it, save every section while the bundle is still open) and pin, for every
 * section, its names, its page count after a save/load round trip, and the text on its pages. The existing
 * PDFExtractorTest only checks section and bookmark names.
 */
class PdfUnbundlingParityTest {

    private final PDFExtractor pdfExtractor = new PDFExtractor();

    private final PDFTestHelper pdfTestHelper = new PDFTestHelper();

    @Test
    void spM05_shouldUnbundleTheIdpcBundleIntoTheJ17Sections() throws IOException {
        assertThat(unbundle(DocumentType.IDPC_CLARK_KENT_PDF, IDPC_FILE_TYPE), contains(
                "Charges | Charges | Charges | pages=2 | text=Charges [ Page No : 1] Charge sheet [ Page No : 2]",
                "Case Summary | Case Summary | Case Summary | pages=2 | text=Case Summary [ Page No : 3] MG5 [ Page No : 4]",
                "Key Witness Statements | MG11 - Apple | MG11 - Apple | pages=1 | text=MG11 - Apple [ Page No : 6]",
                "Key Witness Statements | MG11 - Carrot | MG11 - Carrot | pages=1 | text=MG11 - Carrot [ Page No : 7]",
                "Key Exhibits | Key Exhibits | Key Exhibits | pages=1 | text=Key Exhibits [ Page No : 8]",
                "Transcripts ABE interviews | Transcripts ABE interviews | Transcripts ABE interviews | pages=1 | text=Transcripts ABE interviews [ Page No : 9]",
                "Pre Cons | Pre Cons | Pre Cons | pages=1 | text=Pre Cons [ Page No : 10]",
                "Streamlined Forensic Reports | Streamlined Forensic Reports | Streamlined Forensic Reports | pages=1 | text=Streamlined Forensic Reports [ Page No : 11]"));
    }

    @Test
    void spM05_shouldUnbundleTheIdpcBundleSplittingEverySubSectionIntoTheJ17Sections() throws IOException {
        assertThat(unbundle(DocumentType.IDPC_MULTIPLE_SUB_SECTION, IDPC_SPLIT_ALL_FILE_TYPE), contains(
                "Key Witness Statements | Stmt - Cyril CAULIFLOWER | Stmt - Cyril CAULIFLOWER | pages=1 | text=Stmt - Cyril CAULIFLOWER [ Page No : 2]",
                "Key Witness Statements | Stmt - DS 999 A BOBBY | Stmt - DS 999 A BOBBY | pages=1 | text=Stmt - DS 999 A BOBBY [ Page No : 3]",
                "Key Exhibits | Record Of Recorded Interview Ref:123456 | Record Of Recorded Interview Ref:123456 | pages=1 | text=Record Of Recorded Interview Ref:123456 [ Page No : 5]",
                "Key Exhibits | Record Of Recorded Interview Ref: 245678 | Record Of Recorded Interview Ref: 245678 | pages=1 | text=Record Of Recorded Interview Ref: 245678 [ Page No : 6]",
                "Key Exhibits | Photo of Suspect | Photo of Suspect | pages=1 | text=Photo of Suspect [ Page No : 7]",
                "Key Exhibits | Map of Scene | Map of Scene | pages=1 | text=Map of Scene [ Page No : 8]",
                "Transcripts ABE interviews | Sub Section 1 | Sub Section 1 | pages=1 | text=Sub Section 1 [ Page No : 10]",
                "Transcripts ABE interviews | Sub Section 2 | Sub Section 2 | pages=1 | text=Sub Section 2 [ Page No : 11]",
                "Transcripts ABE interviews | Sub Section 3 | Sub Section 3 | pages=1 | text=Sub Section 3 [ Page No : 12]",
                "Streamlined Forensic Reports | Sub Section 1 | Sub Section 1 | pages=1 | text=Sub Section 1 [ Page No : 14]",
                "Streamlined Forensic Reports | Sub Section 2 | Sub Section 2 | pages=1 | text=Sub Section 2 [ Page No : 15]"));
    }

    @Test
    void spM05_shouldUnbundleTheMagistrateCourtEvidenceBundleIntoTheJ17Sections() throws IOException {
        assertThat(unbundle(DocumentType.MAGISTRATE_COURT_EVIDENCE_PDF, MAGISTRATE_FILE_TYPE), contains(
                "Witness Statements | Anthony Adams | Anthony Adams | pages=1 | text=Anthony Adams [ Page No : 2]",
                "Exhibits | MG6C | MG6C | pages=1 | text=MG6C [ Page No : 4]",
                "Used Materials | Used Materials | Used Materials | pages=2 | text=Used Materials [ Page No : 5] MG7 [ Page No : 6]",
                "Pre Cons | Pre Cons | Pre Cons | pages=2 | text=Pre Cons [ Page No : 7] CASE ITEM1 [ Page No : 8]"));
    }

    private List<String> unbundle(final DocumentType documentType, final String fileType) throws IOException {
        final List<BundleSection> bundleSections = fetchMockReferenceData().get(fileType);
        final List<String> sections = new ArrayList<>();

        try (PDDocument bundle = PDDocument.load(new ByteArrayInputStream(bundleBytes(documentType)))) {
            for (final PDDocumentHolder section : pdfExtractor.splitIntoSections(bundle, bundleSections)) {
                final ByteArrayOutputStream sectionBytes = new ByteArrayOutputStream();
                section.saveDocument(sectionBytes);
                sections.add(describe(section, sectionBytes.toByteArray()));
                section.close();
            }
        }
        return sections;
    }

    private byte[] bundleBytes(final DocumentType documentType) throws IOException {
        try (PDDocument generated = pdfTestHelper.getPDDocument(documentType)) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            generated.save(bytes);
            return bytes.toByteArray();
        }
    }

    private static String describe(final PDDocumentHolder section, final byte[] savedSection) throws IOException {
        try (PDDocument reloaded = PDDocument.load(savedSection)) {
            final String text = new PDFTextStripper().getText(reloaded).trim().replaceAll("\\s+", " ");
            return section.getSectionName() + " | " + section.getDocumentName() + " | " + section.getBookmarkTitle()
                    + " | pages=" + reloaded.getNumberOfPages() + " | text=" + text;
        }
    }
}
