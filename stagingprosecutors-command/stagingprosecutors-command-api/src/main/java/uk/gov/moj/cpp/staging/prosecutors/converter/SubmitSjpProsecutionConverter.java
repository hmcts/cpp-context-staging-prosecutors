package uk.gov.moj.cpp.staging.prosecutors.converter;

import static uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution.submitSjpProsecution;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttp;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import java.util.UUID;

public class SubmitSjpProsecutionConverter implements Converter<Pair<SubmitSjpProsecutionHttp, UUID>, SubmitSjpProsecution> {

    @Override
    public SubmitSjpProsecution convert(Pair<SubmitSjpProsecutionHttp, UUID> source) {
        return submitSjpProsecution()
                .withDefendant(source.getLeft().getDefendant())
                .withProsecutionSubmissionDetails(convertCommandProsecutionSubmissionDetailsToDomain(source.getLeft().getProsecutionSubmissionDetails()))
                .withSubmissionId(source.getRight())
                .build();
    }

    private uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails convertCommandProsecutionSubmissionDetailsToDomain(final SjpProsecutionSubmissionDetails sjpProsecutionSubmissionDetails) {
        return uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails()
                .withInformant(sjpProsecutionSubmissionDetails.getInformant())
                .withUrn(sjpProsecutionSubmissionDetails.getUrn())
                .withProsecutingAuthority(sjpProsecutionSubmissionDetails.getProsecutingAuthority())
                .withWrittenChargePostingDate(sjpProsecutionSubmissionDetails.getWrittenChargePostingDate())
                .build();
    }

}
