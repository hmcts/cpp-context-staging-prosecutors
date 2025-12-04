package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RejectSubmission;
import uk.gov.moj.cps.stagingprosecutors.domain.event.PublicProsecutionRejected;

public class ProsecutionRejectedToRejectSubmissionConverter implements Converter<PublicProsecutionRejected, RejectSubmission> {

    @Override
    public RejectSubmission convert(final PublicProsecutionRejected source) {
        return RejectSubmission
                .rejectSubmission()
                .withSubmissionId(source.getExternalId())
                .withErrors(source.getErrors())
                .withCaseErrors(source.getCaseErrors())
                .withDefendantErrors(source.getDefendantErrors())
                .build();
    }

}
