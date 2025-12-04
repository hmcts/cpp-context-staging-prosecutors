package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared;

import static java.util.Arrays.asList;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UnbundlingConstants {
    public static final String RECORD_DOCUMENT_UNBUNDLE_RESULT = "stagingprosecutors.command.record-document-unbundle-result";
    public static final String RECORD_UNBUNDLE_DOCUMENT_RESULTS  ="stagingprosecutors.command.record-unbundled-document-results";
    public static final String DOCUMENT_IS_NOT_PRESENT_IN_FILE_STORE_FOR_REFERENCE_ID = "Document is not present in File store for reference ID";
    public static final String DOCUMENT_DO_NOT_HAVE_VALID_BOOKMARKS = "Document do not have valid bookmarks";
    public static final String FAILED_TO_RETRIEVE_BUNDLE_FILE_SERVICE_HAS_THROW_AN_EXCEPTION = "Failed to retrieve bundle FileService has throw an exception";
    public static final String ERROR_WHILE_LOADING_PDDOCUMENT_FROM_THE_CONTENT_STREAM_RECEIVED_BY_FILE_SERVICE = "Error while loading PDDocument from the contentStream received by FileService";
    public static final String CASE_ID = "caseId";
    public static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    public static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    public static final String RECEIVED_DATE_TIME = "receivedDateTime";
    public static final String MATERIAL = "material";
    public static final String MATERIALS = "materials";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    public static final String FILE_STORE_ID = "fileStoreId";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String FILE_TYPE = "fileType";
    public static final String FAILED_TO_RETRIEVE_FILE_SERVICE_EXCEPTION = "Failed to retrieve bundle FileService has throw an exception";
    public static final String FILE_NAME = "fileName";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final DateTimeFormatter RECEIVED_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String INVALID_PDF_OUTLINE_EXCEPTION_MSG = "PDF does not have valid book marks, actual bookmarks";
    public static final String MISSING_BOOKMARKS_MSG = "Do not have any bookmarked page for the pdf outline";
    public static final String GENERIC_INVALID_PDF_EXCEPTION_MSG = "Technical error while while un bundling PDF";
    public static final String PROSECUTION_CASE_FILE_ADD_MATERIAL = "prosecutioncasefile.add-material";
    public static final String PROSECUTION_CASE_FILE_ADD_MATERIALS = "prosecutioncasefile.add-materials";
    public static final String FAILED_TO_PROCESS_DOCUMENT_BUNDLE = "Failed to process document bundle ={}";
    public static final String INVALID_CHARS_IN_FILENAME = "[\\~\\\"\\#\\%\\&\\*\\:\\<\\>\\?\\/\\\\\\{\\|\\}]";

    public static final String ACCEPTED_CHAR = "-";

    private static final String PRE_CONS = "Pre Cons";
    private static final String CHARGES = "Charges";

    private UnbundlingConstants() {
    }

    public static List<String> getDefendantNameExceptionList() {
        return asList(PRE_CONS, CHARGES);
    }

}
