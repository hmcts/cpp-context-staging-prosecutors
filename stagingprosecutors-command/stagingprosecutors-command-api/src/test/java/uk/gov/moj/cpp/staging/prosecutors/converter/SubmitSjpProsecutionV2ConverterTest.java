package uk.gov.moj.cpp.staging.prosecutors.converter;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2.submitSjpProsecutionHttpV2;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;

import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitSjpProsecutionV2ConverterTest {

    @InjectMocks
    private SubmitSjpProsecutionV2Converter submitSjpProsecutionV2Converter;

    @Mock
    private UUIDProducer uuidProducer;

    private final UUID submissionId = fromString("d5687ed7-6c11-4d31-8ede-924b3c48442b");

    @Test
    public void shouldConvert() {
        final SubmitSjpProsecutionHttpV2 payload = submitSjpProsecutionHttpV2().build();
        final SubmitSjpProsecution expectedConvertedPayload = SubmitSjpProsecution.submitSjpProsecution()
                .withSubmissionId(submissionId)
                .build();

        final Pair<SubmitSjpProsecutionHttpV2, UUID> source = new ImmutablePair<>(payload, submissionId);
        final SubmitSjpProsecution actualConvertedPayload = submitSjpProsecutionV2Converter.convert(source);

        assertThat(actualConvertedPayload, equalTo(expectedConvertedPayload));
    }

}