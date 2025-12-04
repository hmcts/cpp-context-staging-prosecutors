package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.WitnessPtph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PtphValidator {
    public Map<String, List<String>> validate(final CpsServePtph cpsServePtph, final Map<String, List<String>> validationErrors) {
        final List<WitnessPtph> witnesses = cpsServePtph.getWitnesses();
        if (nonNull(witnesses)) {
            witnesses.forEach(witnessPtph -> {
                if(isNotPresent(ofNullable(witnessPtph.getWitnessFirstName())) && isNotPresent(ofNullable(witnessPtph.getWitnessLastName()))){
                    addError(validationErrors, FieldName.WITNESS_NAME_PTPH.getValue(), "Witness First name or Last name should be present.");
                }
            });
        }

        return validationErrors;
    }

    private boolean isNotPresent(final Optional<String> field) {
        return !field.isPresent();
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
