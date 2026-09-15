package uk.gov.moj.cpp.staging.prosecutors.urn;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.staging.prosecutors.urn.CaseUrnSanitizer.URN_EMPTY_MESSAGE;
import static uk.gov.moj.cpp.staging.prosecutors.urn.CaseUrnSanitizer.URN_NON_ASCII_MESSAGE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class CaseUrnSanitizerTest {

    @Test
    public void shouldReturnUnchangedAsciiUrn() {
        assertThat(CaseUrnSanitizer.sanitize("DVLA12345"), is("DVLA12345"));
    }

    @Test
    public void shouldKeepSplitAndMergedUrnPunctuation() {
        assertThat(CaseUrnSanitizer.sanitize("01AB1234514/2"), is("01AB1234514/2"));
        assertThat(CaseUrnSanitizer.sanitize("01AB1234514 (M)"), is("01AB1234514 (M)"));
        assertThat(CaseUrnSanitizer.sanitize("01AB1234514(M)/2"), is("01AB1234514(M)/2"));
    }

    @Test
    public void shouldStripLeadingZeroWidthSpaceAndAccept() {
        assertThat(CaseUrnSanitizer.sanitize("\u200bDVLA12345"), is("DVLA12345"));
    }

    @Test
    public void shouldStripLeadingZeroWidthSpaceFromDcendnUrnAndAccept() {
        final String contaminatedUrn = "\u200bDCENDN72565221";

        assertThat(contaminatedUrn.codePointAt(0), is(0x200B));
        assertThat(contaminatedUrn, is("\u200bDCENDN72565221"));
        assertThat(CaseUrnSanitizer.sanitize(contaminatedUrn), is("DCENDN72565221"));
        assertThat(CaseUrnSanitizer.wasSanitized(contaminatedUrn, "DCENDN72565221"), is(true));
    }

    @Test
    public void shouldStripZeroWidthSpaceInTheMiddleAndAccept() {
        assertThat(CaseUrnSanitizer.sanitize("DVLA\u200b12345"), is("DVLA12345"));
    }

    @Test
    public void shouldStripBomZwnjZwjAndWordJoiner() {
        assertThat(CaseUrnSanitizer.sanitize("\ufeffDVLA\u200c12\u200d345\u2060"), is("DVLA12345"));
    }

    @Test
    public void shouldReplaceNonBreakingSpaceThenTrim() {
        assertThat(CaseUrnSanitizer.sanitize("\u00a0DVLA12345\u00a0"), is("DVLA12345"));
    }

    @Test
    public void shouldTrimAsciiWhitespace() {
        assertThat(CaseUrnSanitizer.sanitize("  DVLA12345  "), is("DVLA12345"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\u200b", "\u200b\u200b", "\u00a0"})
    public void shouldRejectBlankOrInvisibleOnlyUrn(final String urn) {
        final InvalidCaseUrnException exception = assertThrows(InvalidCaseUrnException.class, () -> CaseUrnSanitizer.sanitize(urn));
        assertThat(exception.getMessage(), is(URN_EMPTY_MESSAGE));
    }

    @Test
    public void shouldRejectVisibleNonAsciiCharacters() {
        final InvalidCaseUrnException exception = assertThrows(InvalidCaseUrnException.class, () -> CaseUrnSanitizer.sanitize("DVLA12345А"));
        assertThat(exception.getMessage(), is(URN_NON_ASCII_MESSAGE));
    }

    @Test
    public void shouldRejectSmartQuotes() {
        final InvalidCaseUrnException exception = assertThrows(InvalidCaseUrnException.class, () -> CaseUrnSanitizer.sanitize("DVLA12345\u2019"));
        assertThat(exception.getMessage(), is(URN_NON_ASCII_MESSAGE));
    }

    @Test
    public void wasSanitizedShouldBeTrueWhenInvisibleCharactersWereRemoved() {
        assertThat(CaseUrnSanitizer.wasSanitized("\u200bDVLA12345", "DVLA12345"), is(true));
        assertThat(CaseUrnSanitizer.wasSanitized("DVLA12345", "DVLA12345"), is(false));
    }
}
