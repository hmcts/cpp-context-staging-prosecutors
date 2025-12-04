package uk.gov.moj.cpp.staging.prosecutors.validators;

import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_GENDER;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_OFFENCE_DATE_CODE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.GENDER_NOT_ACCEPTABLE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_CODE_NOT_ACCEPTABLE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_INVALID_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_INVALID_DURATION;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubmitSjpProsecutionHttpV2Validator {


    public Map<String, List<String>> validate(final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2) {

        final Map<String, List<String>> validationErrors = new HashMap<>();
        final Optional<SjpPerson> defendantPerson = Optional.ofNullable(submitSjpProsecutionHttpV2.getDefendant().getDefendantPerson());

        defendantPerson.ifPresent(sjpPerson -> validateGender(sjpPerson, validationErrors));

        validateOffenceDate(submitSjpProsecutionHttpV2, validationErrors);

        return validationErrors;
    }

    private void validateGender(final SjpPerson sjpPerson, Map<String, List<String>> validationErrors) {
        if (!Arrays.asList(0, 1, 2, 9).contains(sjpPerson.getSelfDefinedInformation().getGender())) {
            addError(validationErrors, FIELD_GENDER, GENDER_NOT_ACCEPTABLE);
        }
    }

    private void validateOffenceDate(final SubmitSjpProsecutionHttpV2 submitSjpProsecutionHttpV2, Map<String, List<String>> validationErrors) {
        final SjpOffence offence = submitSjpProsecutionHttpV2.getDefendant().getOffences().get(0);
        final boolean invalidOffenceDateCode = !Arrays.asList(1, 2, 3, 4, 5, 6).contains(offence.getOffenceDateCode());
        final boolean offenceIsWithinADuration = 4 == offence.getOffenceDateCode();
        final boolean offenceIsWithinADate = !offenceIsWithinADuration;
        final boolean invalidOffenceDuration = offenceIsWithinADuration &&
                (offence.getOffenceCommittedDate() == null || offenceCommittedEndDateIsNotPresentOrItsBeforeCommittedDate(offence));
        final boolean invalidOffenceDate = offenceIsWithinADate && offence.getOffenceCommittedDate() == null;

        if (invalidOffenceDateCode) {
            addError(validationErrors, FIELD_OFFENCE_DATE_CODE, OFFENCE_DATE_CODE_NOT_ACCEPTABLE);
        } else if (invalidOffenceDuration) {
            addError(validationErrors, FIELD_OFFENCE_DATE_CODE, OFFENCE_DATE_INVALID_DURATION);
        } else if (invalidOffenceDate) {
            addError(validationErrors, FIELD_OFFENCE_DATE_CODE, OFFENCE_DATE_INVALID_DATE);
        }
    }

    private boolean offenceCommittedEndDateIsNotPresentOrItsBeforeCommittedDate(final SjpOffence offence) {
        return !Optional.ofNullable(offence.getOffenceCommittedEndDate()).isPresent() || Optional.ofNullable(offence.getOffenceCommittedEndDate()).filter(endDate -> endDate.isBefore(offence.getOffenceCommittedDate())).isPresent();
    }

    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }

}