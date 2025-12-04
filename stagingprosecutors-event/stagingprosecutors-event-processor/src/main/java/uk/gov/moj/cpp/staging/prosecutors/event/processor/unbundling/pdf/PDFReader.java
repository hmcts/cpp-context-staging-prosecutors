package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class PDFReader {

    public List<String> readSections(final PDDocument pdf) {
        final List<String> sections = new ArrayList<>();
        requireNonNull(pdf);
        if (nonNull(pdf.getDocumentCatalog()) && nonNull(pdf.getDocumentCatalog().getDocumentOutline())
                && nonNull(pdf.getDocumentCatalog().getDocumentOutline().children())) {

            final Iterable<PDOutlineItem> pdOutlineItems = pdf.getDocumentCatalog().getDocumentOutline().children();
            pdOutlineItems.forEach(pdOutlineItem -> sections.add(pdOutlineItem.getTitle()));
        }
        return sections;
    }
}
