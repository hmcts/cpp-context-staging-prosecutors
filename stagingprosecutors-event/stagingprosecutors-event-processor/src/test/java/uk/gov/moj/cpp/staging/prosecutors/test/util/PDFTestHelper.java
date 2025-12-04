package uk.gov.moj.cpp.staging.prosecutors.test.util;

import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.IDPC_SPLIT_ALL_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.MAGISTRATE_FILE_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.PDFConstants.SIMPLE_FILE_TYPE;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.util.internal.StringUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PDFTestHelper.class);

    public PDDocument getPDDocument(DocumentType documentType){

        switch (documentType){
            case SIMPLE_BOOKMARK_PDF :
            default:
                return createPDFDocument(getSimpleTemplate());
            case IDPC_CHARGES_AND_PRECON_ONLY :
                return createPDFDocument(getIDPCChargesAndPreConTemplate());
            case IDPC_SINGLE_SUB_SECTION :
                return createPDFDocument(getIDPCSingleSubSectionTemplate());
            case IDPC_MULTIPLE_SUB_SECTION :
                return createPDFDocument(getIDPCMultipleSubSectionTemplate());
            case MULTI_LEVEL_BOOKMARK_PDF :
                return createPDFDocument(getMultiLevelPDFTemplate());
            case IDPC_CLARK_KENT_PDF :
                return createPDFDocument(getIdpcClarkKentTemplate());
            case IDPC_LOIS_LANE_PDF :
                return createPDFDocument(getIdpcLoisLaneTemplate());
            case MAGISTRATE_COURT_EVIDENCE_PDF :
                return createPDFDocument(getMagistrateCourtEvidenceTemplate());
            case INVALID_BOOKMARK_PDF :
                return new PDDocument();
        }
    }

    private static PDDocument createPDFDocument(LinkedHashMap<String, List<String>> bookmarkTemplate) {
        PDDocument pdDocument = new PDDocument();
        try {

            int pageNum = 1;

            PDDocumentOutline documentOutline = new PDDocumentOutline();
            pdDocument.getDocumentCatalog().setDocumentOutline(documentOutline);

            final Set<String> bookmarkKeys = bookmarkTemplate.keySet();
            for(String bookmarkKey : bookmarkKeys){

                final PDPage page = new PDPage();
                addContentsToPage(pageNum, page, bookmarkKey);
                pdDocument.addPage(page);

                PDOutlineItem bookmark = new PDOutlineItem();
                bookmark.setDestination(page);
                bookmark.setTitle(bookmarkKey);

                pageNum++;

                final List<String> childBookmarks = bookmarkTemplate.get(bookmarkKey);
                for(String childBookmarkKey : childBookmarks){

                    final PDPage childPage = new PDPage();
                    addContentsToPage(pageNum, childPage, childBookmarkKey);
                    pdDocument.addPage(childPage);

                    PDOutlineItem childBookmark = new PDOutlineItem();
                    childBookmark.setDestination(childPage);
                    childBookmark.setTitle(childBookmarkKey);

                    pageNum++;

                    bookmark.addLast(childBookmark);
                }

                documentOutline.addLast(bookmark);
            }

        } catch (IOException e) {
            LOGGER.error("Exception while trying to create pdf document {} ", e);
        }
        return pdDocument;
    }

    public static LinkedHashMap<String, List<String>> getIDPCChargesAndPreConTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Charges", Arrays.asList("Charge sheet"));
        bookmarkTemplate.put("Pre Cons",Collections.emptyList());

        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getIDPCSingleSubSectionTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Key Witness Statements",Arrays.asList("Stmt - Cyril CAULIFLOWER"));
        bookmarkTemplate.put("Key Exhibits", Arrays.asList("Record Of Recorded Interview Ref:123456"));
        bookmarkTemplate.put("Transcripts ABE interviews",Arrays.asList("Sub Section 1"));
        bookmarkTemplate.put("Streamlined Forensic Reports",Arrays.asList("Sub Section 1"));

        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getIDPCMultipleSubSectionTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Key Witness Statements",Arrays.asList("Stmt - Cyril CAULIFLOWER", "Stmt - DS 999 A BOBBY"));
        bookmarkTemplate.put("Key Exhibits", Arrays.asList("Record Of Recorded Interview Ref:123456", "Record Of Recorded Interview Ref: 245678", "Photo of Suspect", "Map of Scene"));
        bookmarkTemplate.put("Transcripts ABE interviews",Arrays.asList("Sub Section 1", "Sub Section 2", "Sub Section 3"));
        bookmarkTemplate.put("Streamlined Forensic Reports",Arrays.asList("Sub Section 1", "Sub Section 2"));

        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getSimpleTemplate() {
        LinkedHashMap<String, List<String>> bookmarks = new LinkedHashMap<>();

        bookmarks.put("IDPC_SEC1",Collections.emptyList());
        bookmarks.put("IDPC_SEC2",Collections.emptyList());
        bookmarks.put("IDPC_SEC3",Collections.emptyList());

        return bookmarks;
    }

    public static LinkedHashMap<String, List<String>> getMultiLevelPDFTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("PARENT_IDPC1", Arrays.asList("IDPC_SEC1"));
        bookmarkTemplate.put("PARENT_IDPC2", Arrays.asList("IDPC_SEC2"));
        bookmarkTemplate.put("PARENT_IDPC3", Arrays.asList("IDPC_SEC3"));
        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getIdpcClarkKentTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Charges", Arrays.asList("Charge sheet"));
        bookmarkTemplate.put("Case Summary",Arrays.asList("MG5"));
        bookmarkTemplate.put("Key Witness Statements",Arrays.asList("MG11 - Apple", "MG11 - Carrot"));
        bookmarkTemplate.put("Key Exhibits", Collections.emptyList());
        bookmarkTemplate.put("Transcripts ABE interviews",Collections.emptyList());
        bookmarkTemplate.put("Pre Cons",Collections.emptyList());
        bookmarkTemplate.put("Streamlined Forensic Reports",Collections.emptyList());

        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getIdpcLoisLaneTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Charges", Arrays.asList("Charge sheet"));
        bookmarkTemplate.put("Case Summary",Arrays.asList("MG5"));
        bookmarkTemplate.put("Key Witness Statements",Arrays.asList("Stmt - Cyril CAULIFLOWER", "Stmt - DS 999 A BOBBY"));
        bookmarkTemplate.put("Key Exhibits", Arrays.asList("Record Of Recorded Interview Ref:123456", "Record Of Recorded Interview Ref: 245678", "Photo of Suspect", "Map of Scene"));
        bookmarkTemplate.put("Transcripts ABE interviews",Collections.emptyList());
        bookmarkTemplate.put("Pre Cons",Collections.emptyList());
        bookmarkTemplate.put("Streamlined Forensic Reports",Collections.emptyList());

        return bookmarkTemplate;
    }

    public static LinkedHashMap<String, List<String>> getMagistrateCourtEvidenceTemplate() {
        LinkedHashMap<String, List<String>> bookmarkTemplate = new LinkedHashMap<>();

        bookmarkTemplate.put("Witness Statements",Arrays.asList("Anthony Adams"));
        bookmarkTemplate.put("Exhibits", Arrays.asList("MG6C"));
        bookmarkTemplate.put("Used Materials",Arrays.asList("MG7"));
        bookmarkTemplate.put("Pre Cons",Arrays.asList("CASE ITEM1"));

        return bookmarkTemplate;
    }

    public static Map<String, List<BundleSection>> fetchMockReferenceData() {
        final Map<String, List<BundleSection>> bookmarksByFileType = new HashMap<>();


        bookmarksByFileType.put(
                IDPC_SPLIT_ALL_FILE_TYPE, Arrays.asList(
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("KWS").withBundleSectionName("Key Witness Statements")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("KW").withBundleSectionName("Key Exhibits")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("TAI").withBundleSectionName("Transcripts ABE interviews")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("SFR").withBundleSectionName("Streamlined Forensic Reports")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build()
                ));

        bookmarksByFileType.put(
                IDPC_FILE_TYPE, Arrays.asList(BundleSection.bundleSection()
                                .withBbundleSectionCode("CH").withBundleSectionName("Charges")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("CS").withBundleSectionName("Case Summary")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("KWS").withBundleSectionName("Key Witness Statements")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("KW").withBundleSectionName("Key Exhibits")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("TAI").withBundleSectionName("Transcripts ABE interviews")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("SFR").withBundleSectionName("Streamlined Forensic Reports")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("PC").withBundleSectionName("Pre Cons")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build()
                ));

        bookmarksByFileType.put(
                MAGISTRATE_FILE_TYPE, Arrays.asList(BundleSection.bundleSection()
                                .withBbundleSectionCode("KWS").withBundleSectionName("Witness Statements")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("KW").withBundleSectionName("Exhibits")
                                .withSplitBundleSubSection(Boolean.TRUE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("SFR").withBundleSectionName("Used Materials")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("PC").withBundleSectionName("Pre Cons")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build()
                ));

        bookmarksByFileType.put(
                SIMPLE_FILE_TYPE, Arrays.asList(BundleSection.bundleSection()
                                .withBbundleSectionCode("SEC1").withBundleSectionName("IDPC_SEC1")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("SEC2").withBundleSectionName("IDPC_SEC2")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build(),
                        BundleSection.bundleSection()
                                .withBbundleSectionCode("SEC3").withBundleSectionName("IDPC_SEC3")
                                .withSplitBundleSubSection(Boolean.FALSE)
                                .build()
                )
        );


        return bookmarksByFileType;
    }

    private static void addContentsToPage(final int pageNum, final PDPage page, String content) throws IOException {
        try (final PDPageContentStream contents = new PDPageContentStream(new PDDocument(), page)) {
            contents.beginText();
            contents.newLineAtOffset(100, 700);
            contents.setFont(PDType1Font.HELVETICA, 12);
            contents.showText(StringUtil.isNullOrEmpty(content) ? " Some content " : content);
            contents.showText("  [ Page No : " + pageNum + "]  ");
            contents.endText();
        }
    }

}
