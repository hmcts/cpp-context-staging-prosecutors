package uk.gov.moj.cpp.staging.prosecutors.urn;

import static java.lang.Character.CONTROL;
import static java.lang.Character.FORMAT;
import static java.lang.Character.LINE_SEPARATOR;
import static java.lang.Character.PARAGRAPH_SEPARATOR;
import static java.lang.Character.PRIVATE_USE;
import static java.lang.Character.SPACE_SEPARATOR;
import static java.lang.Character.SURROGATE;
import static java.lang.Character.charCount;
import static java.lang.Character.getType;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Removes invisible Unicode contamination from a case URN, then rejects any remaining
 * non-ASCII characters. Visible ASCII punctuation used in split/merged URNs is kept.
 */
public final class CaseUrnSanitizer {

    public static final String URN_EMPTY_MESSAGE = "URN must not be empty";
    public static final String URN_NON_ASCII_MESSAGE = "URN contains non-ASCII characters";

    private static final int MIN_PRINTABLE_ASCII = 0x20;
    private static final int MAX_PRINTABLE_ASCII = 0x7E;

    private CaseUrnSanitizer() {
        // utility
    }

    public static String sanitize(final String urn) {
        if (isBlank(urn)) {
            throw new InvalidCaseUrnException(URN_EMPTY_MESSAGE);
        }

        final StringBuilder cleaned = new StringBuilder(urn.length());
        for (int index = 0; index < urn.length(); ) {
            final int codePoint = urn.codePointAt(index);
            index += charCount(codePoint);
            appendSanitizedCodePoint(cleaned, codePoint);
        }

        final String trimmed = cleaned.toString().trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCaseUrnException(URN_EMPTY_MESSAGE);
        }
        if (containsNonPrintableAscii(trimmed)) {
            throw new InvalidCaseUrnException(URN_NON_ASCII_MESSAGE);
        }
        return trimmed;
    }

    public static boolean wasSanitized(final String originalUrn, final String sanitizedUrn) {
        return originalUrn != null && !originalUrn.equals(sanitizedUrn);
    }

    private static void appendSanitizedCodePoint(final StringBuilder cleaned, final int codePoint) {
        final int type = getType(codePoint);
        if (isInvisibleOrFormat(type)) {
            return;
        }
        if (isNonAsciiWhitespace(codePoint, type)) {
            cleaned.append(' ');
            return;
        }
        cleaned.appendCodePoint(codePoint);
    }

    private static boolean isInvisibleOrFormat(final int type) {
        return type == CONTROL || type == FORMAT || type == SURROGATE || type == PRIVATE_USE;
    }

    private static boolean isNonAsciiWhitespace(final int codePoint, final int type) {
        return (type == SPACE_SEPARATOR && codePoint != ' ')
                || type == LINE_SEPARATOR
                || type == PARAGRAPH_SEPARATOR;
    }

    private static boolean containsNonPrintableAscii(final String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint < MIN_PRINTABLE_ASCII || codePoint > MAX_PRINTABLE_ASCII);
    }
}
