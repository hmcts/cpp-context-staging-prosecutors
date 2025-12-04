package uk.gov.moj.cpp.staging.prosecutors.converter;

import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp.submitSjpProsecutionHttp;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;

import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitSjpProsecutionConverterTest {

    @InjectMocks
    private SubmitSjpProsecutionConverter submitSjpProsecutionConverter;

    @Mock
    private UUIDProducer uuidProducer;

    private final UUID submissionId = fromString("d5687ed7-6c11-4d31-8ede-924b3c48442b");

    @Test
    public void shouldConvert() {
        final String URN = "URN";
        final String PROSECUTING_AUTHORITY = "Prosecuting Authority";
        final LocalDate LOCAL_DATE = now();

        final SubmitSjpProsecutionHttp payload = submitSjpProsecutionHttp()
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails()
                        .withInformant(null)
                        .withUrn(URN)
                        .withWrittenChargePostingDate(LOCAL_DATE)
                        .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                        .build())
                .build();
        final SubmitSjpProsecution expectedConvertedPayload = SubmitSjpProsecution.submitSjpProsecution()
                .withSubmissionId(submissionId)
                .withProsecutionSubmissionDetails(SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails()
                        .withInformant(null)
                        .withUrn(URN)
                        .withWrittenChargePostingDate(LOCAL_DATE)
                        .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                        .build())
                .build();

        final Pair< SubmitSjpProsecutionHttp, UUID> source = new ImmutablePair<>(payload,submissionId);
        final SubmitSjpProsecution actualConvertedPayload = submitSjpProsecutionConverter.convert(source);

        assertThat(actualConvertedPayload, equalTo(expectedConvertedPayload));
    }

}