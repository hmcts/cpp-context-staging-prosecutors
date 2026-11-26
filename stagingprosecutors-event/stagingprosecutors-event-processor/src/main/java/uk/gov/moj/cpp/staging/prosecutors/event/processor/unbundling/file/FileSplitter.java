package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDDocumentHolder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFExtractor;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFReader;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf.PDFValidator;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service.ReferenceDataService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.pdfbox.pdmodel.PDDocument;

@SuppressWarnings("squid:S1160")
public class FileSplitter {

    @Inject
    private PDFExtractor pdfExtractor;

    @Inject
    private PDFReader pdfReader;

    @Inject
    private PDFValidator pdfValidator;

    @Inject
    private ReferenceDataService referenceDataService;

    public List<PDDocumentHolder> split(final PDDocument pdDocument, final Integer materialType) {
        final List<String> pdDocumentSections = pdfReader.readSections(pdDocument);
        final List<BundleSection> bundleSections = referenceDataService.getPDFBundleDetails(materialType).getBundleSections();
        final List<String> expectedSections = bundleSections.stream().map(BundleSection::getBundleSectionName).collect(Collectors.toList());

        pdfValidator.validateSections(pdDocumentSections, expectedSections);
        return pdfExtractor.splitIntoSections(pdDocument, bundleSections);
    }
}
