package uk.gov.moj.cpp.staging.prosecutors.event.processor.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ProsecutorCaseReferenceUtilTest {

    @Test
    public void shouldReturnReference() {
        assertThat(ProsecutorCaseReferenceUtil.getProsecutorCaseReference("00TFLA1", "someCaseUrn"), is("00TFLA1:someCaseUrn"));
    }

    @Test
    public void shouldReturnUrnWhenProsecutorAuthorityMissing() {
        assertThat(ProsecutorCaseReferenceUtil.getProsecutorCaseReference(null, "someCaseUrn"), is("someCaseUrn"));
    }

    @Test
    public void shouldNotComputeWhenCaseUrnMissing() {
        assertThrows(InvalidCaseUrnProvided.class, () -> ProsecutorCaseReferenceUtil.getProsecutorCaseReference("00TFLA1", null));
    }
}