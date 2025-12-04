package uk.gov.moj.cpp.staging.prosecutors.unbundling.pdf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_SPLIT_ALL_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.MAGISTRATE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.fetchMockReferenceData;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getIDPCChargesAndPreConTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getIDPCMultipleSubSectionTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getIDPCSingleSubSectionTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getIdpcClarkKentTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getIdpcLoisLaneTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getMagistrateCourtEvidenceTemplate;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.getSimpleTemplate;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.UnBundlingTechnicalException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFExtractor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.test.util.DocumentType;
import uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
@SuppressWarnings({"squid:S1607"})
@ExtendWith(MockitoExtension.class)
public class PDFExtractorTest {
    private Map<String, List<BundleSection>> bookmarksByFileType = new HashMap<>();

    @InjectMocks
    private PDFExtractor pdfExtractor;

    @Spy
    PDFTestHelper pdfTestHelper;

    @BeforeEach
    public void setUp() {
        bookmarksByFileType = fetchMockReferenceData();
    }

    @Test
    public void shouldValidateAndSplitIDPCChargesAndPreCon() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.IDPC_CHARGES_AND_PRECON_ONLY)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(IDPC_FILE_TYPE),
                    getIDPCChargesAndPreConTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitIDPCSingleSubSectionDocument() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.IDPC_SINGLE_SUB_SECTION)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(IDPC_SPLIT_ALL_FILE_TYPE),
                    getIDPCSingleSubSectionTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitIDPCMultipleSubSectionDocument() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.IDPC_MULTIPLE_SUB_SECTION)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(IDPC_SPLIT_ALL_FILE_TYPE),
                    getIDPCMultipleSubSectionTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitIDPCSmithJhon() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.INVALID_BOOKMARK_PDF)) {
            assertThrows(UnBundlingTechnicalException.class, () -> splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(SIMPLE_FILE_TYPE),
                    getSimpleTemplate()));
        }
    }

    @Test
    public void shouldValidateAndSplitIDPCClarkKentPDF() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.IDPC_CLARK_KENT_PDF)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(IDPC_FILE_TYPE),
                    getIdpcClarkKentTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitIDPCLoisLanePDF() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.IDPC_LOIS_LANE_PDF)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(IDPC_FILE_TYPE),
                    getIdpcLoisLaneTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitMagistrateCourtEvidencePDF() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.MAGISTRATE_COURT_EVIDENCE_PDF)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(MAGISTRATE_FILE_TYPE),
                    getMagistrateCourtEvidenceTemplate());
        }
    }

    @Test
    public void shouldValidateAndSplitPDFForSimpleFile() throws IOException {
        try (final PDDocument pdDocument = pdfTestHelper.getPDDocument(DocumentType.SIMPLE_BOOKMARK_PDF)) {
            splitPDFAndAssert(
                    pdDocument,
                    bookmarksByFileType.get(SIMPLE_FILE_TYPE),
                    getSimpleTemplate());
        }
    }

    private void splitPDFAndAssert(final PDDocument pdDocument, final List<BundleSection> expectedParentSections, LinkedHashMap<String, List<String>> expectedParentChildSectionMap) {
        final List<PDDocumentHolder> unbundledPDDocs = pdfExtractor.splitIntoSections(pdDocument, expectedParentSections);
        List<String> expectedSectionNames = getSectionNames(expectedParentSections);

        final Map<String, Integer> initialChildCountForEachParent = new HashMap<>();
        expectedParentChildSectionMap.forEach((key, value) -> initialChildCountForEachParent.put(key, value.size()));

        unbundledPDDocs.forEach(unbundledDoc -> {
            final String parentSectionName = unbundledDoc.getSectionName();
            final String documentName = unbundledDoc.getDocumentName();
            final List<String> childDocumentNames = expectedParentChildSectionMap.get(parentSectionName);

            assertThat("Should assert 'section name' or 'folder name' is present in reference data",
                    expectedSectionNames.contains(parentSectionName), is(true));

            final String bookmarkTitle = unbundledDoc.getBookmarkTitle();

            if (!childDocumentNames.isEmpty() && isSplitEnabled(expectedParentSections, parentSectionName)) {
                initialChildCountForEachParent.put(parentSectionName, initialChildCountForEachParent.get(parentSectionName) - 1);
                assertChildDocument(unbundledDoc, parentSectionName, documentName, childDocumentNames, bookmarkTitle);
            } else {
                initialChildCountForEachParent.put(parentSectionName, 0);
                assertParentDocument(expectedParentChildSectionMap, expectedSectionNames, unbundledDoc, documentName, bookmarkTitle);
            }
        });

        assertThat("Should assert all the child document where extracted so count is expected to be zero",
                initialChildCountForEachParent.values().stream().mapToInt(Integer::intValue).sum(),
                is(0));

    }

    private List<String> getSectionNames(final List<BundleSection> expectedParentSections) {
        return expectedParentSections
                .stream()
                .map(BundleSection::getBundleSectionName)
                .collect(Collectors.toList());
    }

    private void assertParentDocument(final LinkedHashMap<String, List<String>> expectedParentChildSectionMap, final List<String> expectedSectionNames, final PDDocumentHolder unbundledDoc, final String documentName, final String bookmarkTitle) {
        assertThat("Should assert parent document name", expectedSectionNames.contains(documentName), is(true));
        assertThat("Should assert parent document bookmark title", expectedSectionNames.contains(bookmarkTitle), is(true));
        assertThat("Should assert parent document page size ( All test pages created are size one only )",
                unbundledDoc.getPages().getCount(), is(expectedParentChildSectionMap.get(documentName).size() + 1));
    }

    private void assertChildDocument(final PDDocumentHolder unbundledDoc, final String parentSectionName, final String documentName, final List<String> childDocumentNames, final String bookmarkTitle) {
        assertThat("Should assert child document name", childDocumentNames.contains(documentName), is(true));
        assertThat("Should assert child document bookmark title", childDocumentNames.contains(bookmarkTitle), is(true));
        assertThat("Should assert child document page size ( All test pages created are size one only )",
                unbundledDoc.getPages().getCount(), is(1));
    }

    private Boolean isSplitEnabled(final List<BundleSection> expectedSections, final String parentSectionName) {
        final Optional<BundleSection> bundleSectionOptional = expectedSections.stream()
                .filter(expectedSection -> expectedSection.getBundleSectionName()
                        .equals(parentSectionName)).findFirst();
        return bundleSectionOptional.isPresent() && bundleSectionOptional.get().getSplitBundleSubSection();
    }

}
