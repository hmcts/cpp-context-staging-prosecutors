package uk.gov.moj.cpp.staging.prosecutors.validators;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServeBcm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class BcmValidatorTest {

    private BcmValidator bcmValidator = new BcmValidator();

    @Test
    public void validateBcmMandatoryFieldsProvided() {
        final CpsServeBcm cpsServeBcm = CpsServeBcm.cpsServeBcm().withEvidencePostPTPH("some text").build();
        final Map<String, List<String>> actualValidations = bcmValidator.validate(cpsServeBcm, new HashMap<>());

        thenValidationFailsWith(actualValidations, newArrayList());
    }

    @Test
    public void validateBcmMandatoryFieldsNotProvided() {
        final CpsServeBcm cpsServeBcm = CpsServeBcm.cpsServeBcm().build();
        final Map<String, List<String>> actualValidations = bcmValidator.validate(cpsServeBcm, new HashMap<>());

        final String invalidProsecutorDefendantIdsMessage = "Evidence Post PTPH or Evidence Pre PTPH or Other Information fields are missing";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of(FieldName.EVIDENCE_POST_PTPH.getValue(), invalidProsecutorDefendantIdsMessage));

        thenValidationFailsWith(actualValidations, errors);
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final List<Pair<String, String>> errors) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        errors.forEach(error -> violationsMap.put(error.getKey(), singletonList(error.getValue())));
        assertEquals(actualValidations, violationsMap);
    }

}