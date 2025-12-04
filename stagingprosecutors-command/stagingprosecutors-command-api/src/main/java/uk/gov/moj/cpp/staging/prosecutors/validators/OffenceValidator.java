package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_OFFENCE_OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OffenceValidator {

    public Map<String, List<String>> validate(final List<OffenceDetails> offenceDetails, final Map<String, List<String>> validationErrors) {
        validateUniqueOffenceSequenceNumber(offenceDetails, validationErrors);

        return validationErrors;
    }

    private Map<String, List<String>> validateUniqueOffenceSequenceNumber(final List<OffenceDetails> offenceDetails, final Map<String, List<String>> validationErrors) {
        final String nonUniqueOffences = offenceDetails
                .stream()
                .collect(
                        groupingBy(OffenceDetails::getOffenceSequenceNo,
                                mapping(OffenceDetails::getCjsOffenceCode, toList())
                        )).entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getValue().toString())
                .collect(joining(", "));

        if (!nonUniqueOffences.isEmpty()) {
            addError(validationErrors, FIELD_OFFENCE_OFFENCE_SEQUENCE_NO, format(OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE, nonUniqueOffences));
        }

        return validationErrors;
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
