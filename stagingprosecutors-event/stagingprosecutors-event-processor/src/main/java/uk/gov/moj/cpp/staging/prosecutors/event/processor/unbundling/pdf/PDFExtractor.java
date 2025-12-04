package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.apache.commons.lang3.exception.ExceptionUtils;

import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.GENERIC_INVALID_PDF_EXCEPTION_MSG;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.MISSING_BOOKMARKS_MSG;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.UnBundlingTechnicalException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.multipdf.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Requirement is to catch all the exception and raise un bundle failed event so I am suppressing
 * S2629
 */
@SuppressWarnings({"squid:S1166", "squid:S2221"})
public class PDFExtractor {

    private static final int INDEX_OFFSET = 1;
    private static final int FIRST_PAGE_INDEX = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFExtractor.class);

    @SuppressWarnings("squid:S2139") // Suppresses warning about rethrowing logged exception
    public List<PDDocumentHolder> splitIntoSections(PDDocument pdDocument, final List<BundleSection> bundleSections) {
        try {
            final Iterable<PDOutlineItem> pdOutlineItems = pdDocument.getDocumentCatalog().getDocumentOutline().children();
            return new ArrayList<>(fetchOutlineRecursively(bundleSections, pdDocument, pdOutlineItems, null, null));
        } catch (Exception e) {
            LOGGER.error(String.format("%s, Document ID: %d, Number of Pages: %d, Bundle Sections: %d",
                    GENERIC_INVALID_PDF_EXCEPTION_MSG,
                    pdDocument.getDocumentId(),
                    pdDocument.getNumberOfPages(),
                    bundleSections.size()));

            LOGGER.error(ExceptionUtils.getStackTrace(e));
            throw new UnBundlingTechnicalException(GENERIC_INVALID_PDF_EXCEPTION_MSG, e);
        }
    }

    @SuppressWarnings("squid:S2139") // Suppresses warning about rethrowing logged exception
    private List<PDDocumentHolder> fetchOutlineRecursively(final List<BundleSection> bundleSections,
                                                           final PDDocument pdDocument,
                                                           final Iterable<PDOutlineItem> pdOutlineItems,
                                                           final PDOutlineItem pdOutlineItemSibling,
                                                           final String sectionName) throws IOException {
        final List<PDDocumentHolder> pdDocumentsHolders = new ArrayList<>();
        for (final PDOutlineItem pdOutlineItem : pdOutlineItems) {
            try {
                final Optional<BundleSection> bundleSection = bundleSections.stream()
                        .filter(bs -> bs.getBundleSectionName().equals(pdOutlineItem.getTitle()))
                        .findFirst();

                final boolean sectionAbsent = !bundleSection.isPresent();
                final boolean sectionPresentAndRequiresSubSectionSplit = bundleSection.isPresent() && bundleSection.get().getSplitBundleSubSection();
                if (pdOutlineItem.hasChildren() && (sectionAbsent || sectionPresentAndRequiresSubSectionSplit)) {
                    pdDocumentsHolders.addAll(fetchOutlineRecursively(bundleSections, pdDocument, pdOutlineItem.children(), pdOutlineItem.getNextSibling(), pdOutlineItem.getTitle()));
                } else {
                    final PDDocument documentSection = extractSection(pdDocument, pdOutlineItem, pdOutlineItemSibling);
                    final PDDocumentHolder documentHolder = new PDDocumentHolder(pdOutlineItem.getTitle(), documentSection, nonNull(sectionName) ? sectionName : pdOutlineItem.getTitle());
                    pdDocumentsHolders.add(documentHolder);

                }
            } catch (Exception e) {
                LOGGER.error(String.format("%s, Document ID: %d, Failed while adding document holder number %d, %s",
                        GENERIC_INVALID_PDF_EXCEPTION_MSG,
                        pdDocument.getDocumentId(),
                        pdDocumentsHolders.size(),
                        pdOutlineItem.getTitle()));

                LOGGER.error(ExceptionUtils.getStackTrace(e));
                throw new UnBundlingTechnicalException(GENERIC_INVALID_PDF_EXCEPTION_MSG, e);
            }
        }
        return pdDocumentsHolders;
    }

    private PDDocument extractSection(PDDocument pdDocument, PDOutlineItem pdOutlineItem,
                                      PDOutlineItem pdOutlineItemSibling) throws IOException {
        final PageExtractor pageExtractor = new PageExtractor(pdDocument);

        final PDPageTree pages = pdDocument.getDocumentCatalog().getPages();

        final int startPageIndex = pages.indexOf(findBookmarkedPage(pdDocument, pdOutlineItem)) + INDEX_OFFSET;

        pageExtractor.setStartPage(startPageIndex);
        pageExtractor.setEndPage(getSectionEndPageIndex(pdDocument, pdOutlineItem, pdOutlineItemSibling, pages));

        final PDDocument extractedDocument = pageExtractor.extract();
        final PDDocumentOutline pdDocumentOutline = extractOutline(pdDocument, extractedDocument, pdOutlineItem);
        extractedDocument.getDocumentCatalog().setDocumentOutline(pdDocumentOutline);

        return extractedDocument;
    }

    private PDPage findBookmarkedPage(final PDDocument pdDocument, final PDOutlineItem pdOutlineItem) throws IOException {
        final PDPage sectionPage = pdOutlineItem.findDestinationPage(pdDocument);
        if (isNull(sectionPage)) {
            LOGGER.error(MISSING_BOOKMARKS_MSG);
            LOGGER.error(pdOutlineItem.getTitle());
        }
        return sectionPage;
    }

    private int getSectionEndPageIndex(final PDDocument pdDocument, final PDOutlineItem pdOutlineItem,
                                       final PDOutlineItem pdOutlineItemSibling, final PDPageTree pages) throws IOException {
        final int endPageIndex;
        final PDOutlineItem pdOutlineNextItem = pdOutlineItem.getNextSibling();
        if (pdOutlineNextItem != null) {
            endPageIndex = pages.indexOf(findBookmarkedPage(pdDocument, pdOutlineNextItem));
        } else if (pdOutlineItemSibling != null) {
            endPageIndex = pages.indexOf(pdOutlineItemSibling.findDestinationPage(pdDocument));
        } else {
            endPageIndex = pages.getCount();
        }
        return endPageIndex;
    }

    private PDDocumentOutline extractOutline(final PDDocument pdOriginalDoc, final PDDocument pdExtractedDoc,
                                             final PDOutlineItem pdOutlineItem) throws IOException {

        final int originalStartIndex = pdOriginalDoc.getPages().indexOf(pdOutlineItem.findDestinationPage(pdOriginalDoc));

        final PDDocumentOutline pdNewDocumentOutline = new PDDocumentOutline();

        final PDOutlineItem pdNewOutlineItem = new PDOutlineItem();
        pdNewOutlineItem.setTitle(pdOutlineItem.getTitle());

        final PDPageDestination dest = new PDPageFitWidthDestination();
        dest.setPage(pdExtractedDoc.getPage(FIRST_PAGE_INDEX));
        pdNewOutlineItem.setDestination(dest);

        if (nonNull(pdOutlineItem.children())) {
            for (final PDOutlineItem pdSubsectionOutlineItem : pdOutlineItem.children()) {
                extractSubSectionOutline(pdOriginalDoc, pdExtractedDoc, originalStartIndex,
                        pdNewOutlineItem, pdSubsectionOutlineItem);
            }
        }

        pdNewDocumentOutline.addLast(pdNewOutlineItem);
        return pdNewDocumentOutline;
    }

    private void extractSubSectionOutline(final PDDocument pdOriginalDoc,
                                          final PDDocument pdExtractedDoc,
                                          final int originalStartIndex,
                                          final PDOutlineItem pdNewOutlineItem,
                                          final PDOutlineItem pdSubsectionOutlineItem) throws IOException {

        final PDOutlineItem pdNewSubSectionOutlineItem = new PDOutlineItem();
        pdNewSubSectionOutlineItem.setTitle(pdSubsectionOutlineItem.getTitle());
        final PDPageDestination subSectionDest = new PDPageFitWidthDestination();

        final PDPage originalSubSectionPage = pdSubsectionOutlineItem.findDestinationPage(pdOriginalDoc);
        final int originalSubSectionStartIndex = pdOriginalDoc.getPages().indexOf(originalSubSectionPage);
        final int relativePageIndex = originalSubSectionStartIndex - originalStartIndex;

        subSectionDest.setPage(pdExtractedDoc.getPage(relativePageIndex));

        pdNewSubSectionOutlineItem.setDestination(subSectionDest);
        pdNewOutlineItem.addLast(pdNewSubSectionOutlineItem);
    }
}
