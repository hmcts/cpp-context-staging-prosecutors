package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.command.api.CpsServePtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.WitnessPtph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class PtphValidatorTest {

    public static final String ERROR_MESSAGE = "Witness First name or Last name should be present.";
    public static final String LAST_NAME = "lastName";
    public static final String FIRST_NAME = "firstName";
    private PtphValidator ptphValidator = new PtphValidator();

    @Test
    public void validateReturnEmptyErrorListWhenThereIsNoWitness() {
        final CpsServePtph cpsServePtph = CpsServePtph.cpsServePtph().build();
        final Map<String, List<String>> actualValidations = ptphValidator.validate(cpsServePtph, new HashMap<>());
        assertThat(actualValidations.size(), is(0));
    }

    @Test
    public void validateReturnEmptyErrorListWhenWitnessHasFirstNameAndLastName() {
        final CpsServePtph cpsServePtph = CpsServePtph.cpsServePtph().withWitnesses(singletonList(WitnessPtph.witnessPtph().withWitnessFirstName(FIRST_NAME).withWitnessLastName(LAST_NAME).build())).build();
        final Map<String, List<String>> actualValidations = ptphValidator.validate(cpsServePtph, new HashMap<>());
        assertThat(actualValidations.size(), is(0));
    }

    @Test
    public void validateReturnEmptyErrorListWhenWitnessHasOnlyFirstName() {
        final CpsServePtph cpsServePtph = CpsServePtph.cpsServePtph().withWitnesses(singletonList(WitnessPtph.witnessPtph().withWitnessLastName(LAST_NAME).build())).build();
        final Map<String, List<String>> actualValidations = ptphValidator.validate(cpsServePtph, new HashMap<>());
        assertThat(actualValidations.size(), is(0));

    }

    @Test
    public void validateReturnAnErrorListWhenWitnessFirstNameAndLastNameIsMissing() {
        final CpsServePtph cpsServePtph = CpsServePtph.cpsServePtph().withWitnesses(singletonList(WitnessPtph.witnessPtph().build())).build();
        final Map<String, List<String>> actualValidations = ptphValidator.validate(cpsServePtph, new HashMap<>());
        assertThat(actualValidations.size(), is(1));
        assertThat(actualValidations.get(FieldName.WITNESS_NAME_PTPH.getValue()).get(0), is(ERROR_MESSAGE));
    }

}