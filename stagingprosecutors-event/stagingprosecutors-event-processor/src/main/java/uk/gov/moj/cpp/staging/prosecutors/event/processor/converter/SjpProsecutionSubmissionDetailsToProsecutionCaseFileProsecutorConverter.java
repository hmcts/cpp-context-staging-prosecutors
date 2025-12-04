package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;

public class SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter implements Converter<SjpProsecutionSubmissionDetails, Prosecutor> {

    @Override
    public Prosecutor convert(SjpProsecutionSubmissionDetails submissionDetails) {
        return prosecutor()
                .withInformant(submissionDetails.getInformant())
                .withProsecutingAuthority(submissionDetails.getProsecutingAuthority())
                .build();
    }
}
