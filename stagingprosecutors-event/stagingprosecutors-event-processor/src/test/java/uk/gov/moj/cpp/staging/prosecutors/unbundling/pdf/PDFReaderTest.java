package uk.gov.moj.cpp.staging.prosecutors.unbundling.pdf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.fetchMockReferenceData;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFReader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.test.util.DocumentType;
import uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PDFReaderTest {

    private Map<String, List<BundleSection>> bookmarksByFileType = new HashMap<>();
    private List<String> expectedPdfSections;

    @InjectMocks
    private PDFReader pdfReader;

    @Spy
    PDFTestHelper pdfTestHelper;

    @BeforeEach
    public void setUp() {
        bookmarksByFileType = fetchMockReferenceData();
        List<BundleSection> bundleSections = bookmarksByFileType.get(SIMPLE_FILE_TYPE);
        expectedPdfSections = bundleSections.stream().map(BundleSection::getBundleSectionName).collect(Collectors.toList());
    }

    @Test
    public void shouldReadPDFSections() {
        final List<String> actualSectionNames = pdfReader.readSections(pdfTestHelper.getPDDocument(DocumentType.SIMPLE_BOOKMARK_PDF));

        assertThat(actualSectionNames.size(), is(expectedPdfSections.size()));
        assertThat(actualSectionNames, containsInAnyOrder(expectedPdfSections.toArray()));
    }

    @Test
    public void shouldThrowNullPointerExceptionException() {
        assertThrows(NullPointerException.class, () -> pdfReader.readSections(null));
    }
}