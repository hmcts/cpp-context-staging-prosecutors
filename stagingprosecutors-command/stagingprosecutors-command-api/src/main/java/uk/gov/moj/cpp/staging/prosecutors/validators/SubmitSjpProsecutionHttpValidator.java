package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_GENDER;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_OFFENCE_DATE_CODE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.FIELD_OFFENCE_OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.GENDER_NOT_ACCEPTABLE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_CODE_NOT_ACCEPTABLE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_INVALID_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_DATE_INVALID_DURATION;
import static uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionTextResources.OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubmitSjpProsecutionHttpValidator {

    public Map<String, List<String>> validate(final SubmitSjpProsecutionHttp submitSjpProsecutionHttp) {

        final Map<String, List<String>> validationErrors = new HashMap<>();

        validateOffenceSequenceNumber(submitSjpProsecutionHttp, validationErrors);

        SjpPerson defendantPerson = submitSjpProsecutionHttp.getDefendant().getDefendantPerson();
        Optional.ofNullable(defendantPerson).ifPresent(sjpPerson -> validateGender(sjpPerson, validationErrors));

        validateOffenceDate(submitSjpProsecutionHttp, validationErrors);

        return validationErrors;
    }

    private void validateOffenceSequenceNumber(final SubmitSjpProsecutionHttp submitSjpProsecutionHttp, final Map<String, List<String>> validationErrors) {
        final String nonUniqueOffences = submitSjpProsecutionHttp.getDefendant()
                .getOffences()
                .stream()
                .collect(
                        groupingBy(SjpOffence::getOffenceSequenceNo,
                                mapping(SjpOffence::getCjsOffenceCode, toList())
                        )).entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getValue().toString())
                .collect(joining(", "));

        if (!nonUniqueOffences.isEmpty()) {
            addError(validationErrors, FIELD_OFFENCE_OFFENCE_SEQUENCE_NO, format(OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE, nonUniqueOffences));
        }
    }

    private void validateGender(SjpPerson sjpPerson, Map<String, List<String>> validationErrors) {
        if (!Arrays.asList(0, 1, 2, 9).contains(sjpPerson.getSelfDefinedInformation().getGender())) {
            addError(validationErrors, FIELD_GENDER, GENDER_NOT_ACCEPTABLE);
        }
    }

    private void validateOffenceDate(SubmitSjpProsecutionHttp submitSjpProsecutionHttp, Map<String, List<String>> validationErrors) {
        final SjpOffence offence = submitSjpProsecutionHttp.getDefendant().getOffences().get(0);
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