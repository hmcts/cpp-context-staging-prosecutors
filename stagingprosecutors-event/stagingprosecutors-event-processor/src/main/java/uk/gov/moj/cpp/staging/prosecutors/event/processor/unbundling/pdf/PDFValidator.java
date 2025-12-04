package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf;

import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.INVALID_PDF_OUTLINE_EXCEPTION_MSG;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.InvalidPDFOutlineException;

import java.util.List;
import java.util.Optional;

public class PDFValidator {

    public void validateSections(final List<String> actualSections, final List<String> expectedSections) {

        final Optional<String> invalidSection = actualSections.stream()
                .filter(section -> !expectedSections.contains(section))
                .findFirst();

        if (invalidSection.isPresent()) {
            throw new InvalidPDFOutlineException(String.format("%s: %s", INVALID_PDF_OUTLINE_EXCEPTION_MSG, actualSections.toString()));
        }
    }
}
