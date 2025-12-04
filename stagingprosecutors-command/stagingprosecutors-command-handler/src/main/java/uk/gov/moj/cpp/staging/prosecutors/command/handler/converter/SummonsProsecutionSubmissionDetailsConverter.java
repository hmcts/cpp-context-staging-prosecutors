package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails.prosecutionSubmissionDetails;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.SummonsProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;

public class SummonsProsecutionSubmissionDetailsConverter implements Converter<SummonsProsecutionSubmissionDetails, ProsecutionSubmissionDetails> {

    @Override
    public ProsecutionSubmissionDetails convert(final SummonsProsecutionSubmissionDetails summonsProsecutionSubmissionDetails) {
        return prosecutionSubmissionDetails()
                .withSummonsCode(summonsProsecutionSubmissionDetails.getSummonsCode())
                .withInformant(summonsProsecutionSubmissionDetails.getInformant())
                .withCaseMarker(summonsProsecutionSubmissionDetails.getCaseMarker())
                .withUrn(summonsProsecutionSubmissionDetails.getUrn())
                .withProsecutingAuthority(summonsProsecutionSubmissionDetails.getProsecutingAuthority())
                .withHearingDetails(summonsProsecutionSubmissionDetails.getHearingDetails())
                .withInitiationCode(InitiationCode.S)
                .build();
    }
}
