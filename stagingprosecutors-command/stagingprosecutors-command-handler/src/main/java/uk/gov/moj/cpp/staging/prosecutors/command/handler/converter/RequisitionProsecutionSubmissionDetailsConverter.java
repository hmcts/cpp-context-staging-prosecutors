package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails.prosecutionSubmissionDetails;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.RequisitionProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;

public class RequisitionProsecutionSubmissionDetailsConverter implements Converter<RequisitionProsecutionSubmissionDetails, ProsecutionSubmissionDetails> {

    @Override
    public ProsecutionSubmissionDetails convert(final RequisitionProsecutionSubmissionDetails requisitionProsecutionSubmissionDetails) {
        return prosecutionSubmissionDetails()
                .withInformant(requisitionProsecutionSubmissionDetails.getInformant())
                .withCaseMarker(requisitionProsecutionSubmissionDetails.getCaseMarker())
                .withUrn(requisitionProsecutionSubmissionDetails.getUrn())
                .withProsecutingAuthority(requisitionProsecutionSubmissionDetails.getProsecutingAuthority())
                .withHearingDetails(requisitionProsecutionSubmissionDetails.getHearingDetails())
                .withInitiationCode(InitiationCode.Q)
                .withWrittenChargePostingDate(requisitionProsecutionSubmissionDetails.getWrittenChargePostingDate())
                .build();
    }
}
