package uk.gov.moj.cpp.staging.prosecutors.unbundling.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.InvalidPDFOutlineException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file.FileSplitter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFExtractor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFReader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFValidator;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.PDFBundleDetails;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service.ReferenceDataService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileSplitterTest {

    @InjectMocks
    private FileSplitter fileSplitter;

    @Mock
    private PDFExtractor pdfExtractor;

    @Mock
    private PDFReader pdfReader;

    @Mock
    private PDFValidator pdfValidator;

    @Mock
    private PDFBundleDetails pdfBundleDetails;

    @Mock
    private ReferenceDataService referenceDataService;

    private PDDocument pdDocument = new PDDocument();

    @BeforeEach
    public void setUp() {
        when(referenceDataService.getPDFBundleDetails(anyInt())).thenReturn(pdfBundleDetails);
        when(pdfBundleDetails.getBundleSections()).thenReturn(getBundleSections());
        when(pdfReader.readSections(any(PDDocument.class))).thenReturn(new ArrayList<>());
    }

    @Test
    public void shouldSplitSuccessfully() {
        List<PDDocumentHolder> pdDocumentHolders = getPDDocumentHolders();
        when(pdfExtractor.splitIntoSections(any(PDDocument.class), any(List.class))).thenReturn(pdDocumentHolders);
        List<PDDocumentHolder> splitResult = fileSplitter.split(pdDocument, 1);
        assertThat(splitResult.containsAll(pdDocumentHolders), is(true));
    }

    @Test
    public void shouldThrowInvalidPDFOutlineExceptionWhenPdfOutlineIsNotValid() {
        doThrow(InvalidPDFOutlineException.class).when(pdfValidator).validateSections(any(List.class), any(List.class));
        assertThrows(InvalidPDFOutlineException.class, () -> fileSplitter.split(pdDocument, 1));
    }

    @Test
    public void shouldThrowIOException() {
        when(pdfExtractor.splitIntoSections(any(), any())).thenAnswer( invocation -> { throw new IOException(); });
        assertThrows(IOException.class, () -> fileSplitter.split(new PDDocument(), 1));
    }

    private List<PDDocumentHolder> getPDDocumentHolders() {
        PDDocumentHolder pdDocumentHolder = new PDDocumentHolder("documentName", pdDocument, "sectionName");
        List<PDDocumentHolder> pdDocumentHolders = new ArrayList<>();
        pdDocumentHolders.add(pdDocumentHolder);

        return pdDocumentHolders;
    }

    private List<BundleSection> getBundleSections() {
        BundleSection bundleSection = new BundleSection();
        bundleSection.setBundleSectionCode("SectionCode");
        bundleSection.setBundleSectionName("SectionName");
        bundleSection.setSeqNum(1);
        bundleSection.setSplitBundleSubSection(true);

        List<BundleSection> bundleSections = new ArrayList<>();
        bundleSections.add(bundleSection);

        return bundleSections;
    }
}