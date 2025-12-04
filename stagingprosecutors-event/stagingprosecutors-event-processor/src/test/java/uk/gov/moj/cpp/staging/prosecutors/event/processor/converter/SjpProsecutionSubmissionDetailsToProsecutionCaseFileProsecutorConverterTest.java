package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpProsecutionSubmissionDetails;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;

import org.junit.jupiter.api.Test;

public class SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverterTest {


    private SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter converter = new SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter();

    @Test
    public void shouldConvertProsecutionSubmissionDetailsToProsecutionCaseFileProsecutor() {

        final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails = prosecutorsSjpProsecutionSubmissionDetails();
        final Prosecutor prosecutionCaseFileProsecutor = converter.convert(prosecutionSubmissionDetails);

        assertProsecutionCaseFileProsecutorMatchesProsecutionSubmissionDetails(prosecutionCaseFileProsecutor, prosecutionSubmissionDetails);

    }


    public static void assertProsecutionCaseFileProsecutorMatchesProsecutionSubmissionDetails(final Prosecutor prosecutionCaseFileProsecutor,
                                                                                              final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails) {

        assertThat(prosecutionCaseFileProsecutor.getInformant(), is(prosecutionSubmissionDetails.getInformant()));
        assertThat(prosecutionCaseFileProsecutor.getProsecutingAuthority(), is(prosecutionSubmissionDetails.getProsecutingAuthority()));
    }
}