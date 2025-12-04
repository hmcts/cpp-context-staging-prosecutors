package uk.gov.moj.cpp.staging.prosecutors.unbundling.pdf;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFTestHelper.fetchMockReferenceData;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.InvalidPDFOutlineException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFReader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFValidator;
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
public class PDFValidatorTest {

    private Map<String, List<BundleSection>> bookmarksByFileType = new HashMap<>();
    private List<String> actualPdfSections;
    private List<String> expectedPdfSections;

    @InjectMocks
    private PDFValidator pdfValidator;

    @Spy
    PDFTestHelper pdfTestHelper;

    @Spy
    PDFReader pdfReader;

    @BeforeEach
    public void setUp() {
        bookmarksByFileType = fetchMockReferenceData();
        List<BundleSection> bundleSections = bookmarksByFileType.get(SIMPLE_FILE_TYPE);
        actualPdfSections = pdfReader.readSections(pdfTestHelper.getPDDocument(DocumentType.SIMPLE_BOOKMARK_PDF));
        expectedPdfSections = bundleSections.stream().map(BundleSection::getBundleSectionName).collect(Collectors.toList());
    }

    @Test
    public void shouldValidateSectionsWhenActualSectionsExactlyMatchExpected() {
        assertThat(actualPdfSections.size(), is(expectedPdfSections.size()));
        pdfValidator.validateSections(actualPdfSections, expectedPdfSections);
    }

    @Test
    public void shouldThrowInvalidPDFOutlineException() {
        assertThrows(InvalidPDFOutlineException.class, () -> pdfValidator.validateSections(actualPdfSections, singletonList("section")));
    }

    @Test
    public void shouldThrowInvalidPDFOutlineExceptionWhenActualSectionsHaveUnrecognisedSection() {
        actualPdfSections.add("Unknown Section");
        assertThrows(InvalidPDFOutlineException.class, () -> pdfValidator.validateSections(actualPdfSections, expectedPdfSections));
    }
}