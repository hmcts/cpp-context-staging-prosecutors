package uk.gov.moj.cpp.staging.prosecutors.validators;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class DefendantValidatorTest {

    private DefendantValidator defendantValidator = new DefendantValidator();

    @Test
    public void validateNonUniqueProsecutorDefendantIds() {
        final Map<String, List<String>> actualValidations = defendantValidator.validate(getNonUniqueProsecutorDefendantIds(), new HashMap<>());

        final String invalidProsecutorDefendantIdsMessage = "The defendants [ProsecutorDefendantId4, ProsecutorDefendantId4], [ProsecutorDefendantId3, ProsecutorDefendantId3] have same prosecutor defendant ids. Prosecutor defendant ids must be unique.";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of("defendant.defendantDetails.prosecutorDefendantId", invalidProsecutorDefendantIdsMessage));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateUniqueProsecutorDefendantIds() {
        final Map<String, List<String>> actualValidations = defendantValidator.validate(getUniqueProsecutorDefendantIds(), new HashMap<>());

        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }

    private List<DefendantDetails> getNonUniqueProsecutorDefendantIds() {
        final DefendantDetails defendantDetails1 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId1").build();
        final DefendantDetails defendantDetails2 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId2").build();
        final DefendantDetails defendantDetails3 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId3").build();
        final DefendantDetails defendantDetails4 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId3").build();
        final DefendantDetails defendantDetails5 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId4").build();
        final DefendantDetails defendantDetails6 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId4").build();
        return asList(defendantDetails1, defendantDetails2, defendantDetails3, defendantDetails4, defendantDetails5, defendantDetails6);
    }

    private List<DefendantDetails> getUniqueProsecutorDefendantIds() {
        final DefendantDetails defendantDetails1 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId1").build();
        final DefendantDetails defendantDetails2 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId2").build();
        final DefendantDetails defendantDetails3 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId3").build();
        final DefendantDetails defendantDetails4 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId4").build();
        final DefendantDetails defendantDetails5 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId5").build();
        final DefendantDetails defendantDetails6 = defendantDetails().withProsecutorDefendantId("ProsecutorDefendantId6").build();
        return asList(defendantDetails1, defendantDetails2, defendantDetails3, defendantDetails4, defendantDetails5, defendantDetails6);
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final List<Pair<String, String>> errors) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        errors.forEach(error -> violationsMap.put(error.getKey(), singletonList(error.getValue())));
        assertThat(actualValidations, equalTo(violationsMap));
    }

}