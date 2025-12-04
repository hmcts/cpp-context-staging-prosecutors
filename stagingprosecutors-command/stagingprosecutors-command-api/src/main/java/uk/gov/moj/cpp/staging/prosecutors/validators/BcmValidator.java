package uk.gov.moj.cpp.staging.prosecutors.validators;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeBcm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BcmValidator {

    public Map<String, List<String>> validate(final CpsServeBcm cpsServeBcm, final Map<String, List<String>> validationErrors) {

        if (isNotPresent(cpsServeBcm.getEvidencePostPTPH()) && isNotPresent(cpsServeBcm.getEvidencePrePTPH()) && isNotPresent(cpsServeBcm.getOtherInformation())) {
            addError(validationErrors, FieldName.EVIDENCE_POST_PTPH.getValue(), "Evidence Post PTPH or Evidence Pre PTPH or Other Information fields are missing");
        }

        return validationErrors;
    }

    private boolean isNotPresent(final String field) {
        return field == null;
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
