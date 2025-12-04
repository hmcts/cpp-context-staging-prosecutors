package uk.gov.moj.cpp.staging.prosecutors.validators;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails.offenceDetails;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class OffenceValidatorTest {

    private OffenceValidator offenceValidator = new OffenceValidator();

    @Test
    public void validateNonUniqueOffenceSequenceNumbers() {
        final Map<String, List<String>> actualValidations = offenceValidator.validate(getNonUniqueOffenceSequenceNumbers(), new HashMap<>());

        final String invalidOffenceSequenceNoMessage = "The offences [CA03013, CA03014], [CA03015, CA03016] have same offence sequence numbers. Offence sequence numbers must be unique.";
        final List<Pair<String, String>> errors = newArrayList(
                Pair.of("defendant.offences.offenceSequenceNo", invalidOffenceSequenceNoMessage));

        thenValidationFailsWith(actualValidations, errors);
    }

    @Test
    public void validateUniqueOffenceSequenceNumbers() {
        final Map<String, List<String>> actualValidations = offenceValidator.validate(getUniqueOffenceSequenceNumbers(), new HashMap<>());

        assertThat(actualValidations, equalTo(Collections.EMPTY_MAP));
    }

    private List<OffenceDetails> getNonUniqueOffenceSequenceNumbers() {
        final OffenceDetails offenceDetails1 = offenceDetails().withCjsOffenceCode("CA03013").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails2 = offenceDetails().withCjsOffenceCode("CA03014").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails3 = offenceDetails().withCjsOffenceCode("CA03015").withOffenceSequenceNo(2).build();
        final OffenceDetails offenceDetails4 = offenceDetails().withCjsOffenceCode("CA03016").withOffenceSequenceNo(2).build();
        final OffenceDetails offenceDetails5 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(3).build();
        final OffenceDetails offenceDetails6 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(4).build();
        return asList(offenceDetails1, offenceDetails2, offenceDetails3, offenceDetails4, offenceDetails5, offenceDetails6);
    }

    private List<OffenceDetails> getUniqueOffenceSequenceNumbers() {
        final OffenceDetails offenceDetails1 = offenceDetails().withCjsOffenceCode("CA03013").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails2 = offenceDetails().withCjsOffenceCode("CA03014").withOffenceSequenceNo(2).build();
        final OffenceDetails offenceDetails3 = offenceDetails().withCjsOffenceCode("CA03015").withOffenceSequenceNo(3).build();
        final OffenceDetails offenceDetails4 = offenceDetails().withCjsOffenceCode("CA03016").withOffenceSequenceNo(4).build();
        final OffenceDetails offenceDetails5 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(5).build();
        final OffenceDetails offenceDetails6 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(6).build();
        return asList(offenceDetails1, offenceDetails2, offenceDetails3, offenceDetails4, offenceDetails5, offenceDetails6);
    }

    private void thenValidationFailsWith(final Map<String, List<String>> actualValidations, final List<Pair<String, String>> errors) {
        final Map<String, List<String>> violationsMap = new HashMap<>();
        errors.forEach(error -> violationsMap.put(error.getKey(), singletonList(error.getValue())));
        assertThat(actualValidations, equalTo(violationsMap));
    }

}