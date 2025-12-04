package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails.prosecutionSubmissionDetails;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.ChargeProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;

public class ChargeProsecutionSubmissionDetailsConverter implements Converter<ChargeProsecutionSubmissionDetails, ProsecutionSubmissionDetails> {

    @Override
    public ProsecutionSubmissionDetails convert(final ChargeProsecutionSubmissionDetails chargeProsecutionSubmissionDetails) {

        return prosecutionSubmissionDetails()
                .withUrn(chargeProsecutionSubmissionDetails.getUrn())
                .withProsecutingAuthority(chargeProsecutionSubmissionDetails.getProsecutingAuthority())
                .withHearingDetails(chargeProsecutionSubmissionDetails.getHearingDetails())
                .withInformant(chargeProsecutionSubmissionDetails.getInformant())
                .withCaseMarker(chargeProsecutionSubmissionDetails.getCaseMarker())
                .withInitiationCode(InitiationCode.C)
                .build();
    }

}
