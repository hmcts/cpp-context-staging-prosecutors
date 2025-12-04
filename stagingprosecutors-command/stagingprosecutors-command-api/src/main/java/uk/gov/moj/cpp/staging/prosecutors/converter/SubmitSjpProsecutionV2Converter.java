package uk.gov.moj.cpp.staging.prosecutors.converter;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;

import java.util.UUID;

import static uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution.submitSjpProsecution;

public class SubmitSjpProsecutionV2Converter implements Converter<Pair<SubmitSjpProsecutionHttpV2, UUID>, SubmitSjpProsecution> {



    @Override
    public SubmitSjpProsecution convert(Pair<SubmitSjpProsecutionHttpV2, UUID> source) {
        return submitSjpProsecution()
                .withDefendant(source.getLeft().getDefendant())
                .withProsecutionSubmissionDetails(source.getLeft().getProsecutionSubmissionDetails())
                .withSubmissionId(source.getRight())
                .build();
    }

}
