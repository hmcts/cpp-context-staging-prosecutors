package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicProsecutionRejected;

import org.junit.jupiter.api.Test;

public class ProsecutionRejectedProcessorToRejectSubmissionConverterTest {

    @Test
    public void shouldConvertWhenGivenAValidInput() {

        final ProsecutionRejectedToRejectSubmissionConverter converter = new ProsecutionRejectedToRejectSubmissionConverter();
        final PublicProsecutionRejected prosecutionRejected = PublicProsecutionRejected.publicProsecutionRejected().build();
        final RejectSubmission expectedConverted = RejectSubmission.rejectSubmission().withSubmissionId(prosecutionRejected.getExternalId()).build();

        final RejectSubmission actualConverted = converter.convert(prosecutionRejected);

        assertThat(expectedConverted, is(actualConverted));
    }
}