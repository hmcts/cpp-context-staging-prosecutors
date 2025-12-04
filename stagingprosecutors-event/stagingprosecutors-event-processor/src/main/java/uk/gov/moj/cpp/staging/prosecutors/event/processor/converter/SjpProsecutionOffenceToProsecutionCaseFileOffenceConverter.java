package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("squid:S1188")
public class SjpProsecutionOffenceToProsecutionCaseFileOffenceConverter implements Converter<List<SjpOffence>, List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence>> {
    @Override
    public List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> convert(final List<SjpOffence> source) {
        return source.stream()
                .map(offence ->
                        offence()
                                .withBackDuty(ofNullable(offence.getBackDuty()).map(BigDecimal::new).orElse(null))
                                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                                .withBackDutyDateTo(offence.getBackDutyDateTo())
                                .withChargeDate(offence.getChargeDate())
                                .withAppliedCompensation(ofNullable(offence.getProsecutorCompensation()).map(BigDecimal::new).orElse(null))
                                .withOffenceCode(offence.getCjsOffenceCode())
                                .withOffenceCommittedDate(offence.getOffenceCommittedDate())
                                .withOffenceCommittedEndDate(offence.getOffenceCommittedEndDate())
                                .withOffenceDateCode(offence.getOffenceDateCode())
                                .withOffenceLocation(offence.getOffenceLocation())
                                .withOffenceSequenceNumber(offence.getOffenceSequenceNo())
                                .withOffenceWording(offence.getOffenceWording())
                                .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                                .withStatementOfFacts(offence.getStatementOfFacts())
                                .withStatementOfFactsWelsh(offence.getStatementOfFactsWelsh())
                                .withVehicleMake(offence.getVehicleMake())
                                .withVehicleRegistrationMark(offence.getVehicleRegistrationMark())
                                .withProsecutorOfferAOCP(offence.getProsecutorOfferAOCP())
                                .build()
                )
                .collect(toList());
    }
}
