package uk.gov.moj.cpp.staging.prosecutors.validators;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_PROSECUTOR_DEFENDANT_ID;

public class DefendantValidator {

    public Map<String, List<String>> validate(final List<DefendantDetails> defendantDetails, final Map<String, List<String>> validationErrors) {
        validateUniqueDefendantId(defendantDetails, validationErrors);

        return validationErrors;
    }

    private Map<String, List<String>> validateUniqueDefendantId(final List<DefendantDetails> defendantDetails, final Map<String, List<String>> validationErrors) {
        final String nonUniqueDefendants = defendantDetails
                .stream()
                .collect(
                        groupingBy(DefendantDetails::getProsecutorDefendantId,
                                mapping(DefendantDetails::getProsecutorDefendantId, toList())
                        )).entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getValue().toString())
                .collect(joining(", "));

        if (!nonUniqueDefendants.isEmpty()) {
            addError(validationErrors, FIELD_PROSECUTOR_DEFENDANT_ID, format(DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE, nonUniqueDefendants));
        }

        return validationErrors;
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
