package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S1188")
public class OffenceToProsecutionCaseFileOffenceConverter implements Converter<List<Offence>, List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence>> {
    @Override
    public List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> convert(final List<Offence> source) {
        return source.stream()
                .map(offence -> offence()
                        .withOffenceId(UUID.randomUUID())
                        .withBackDuty(ofNullable(offence.getOffenceDetails().getBackDuty()).map(BigDecimal::new).orElse(null))
                        .withBackDutyDateFrom(offence.getOffenceDetails().getBackDutyDateFrom())
                        .withBackDutyDateTo(offence.getOffenceDetails().getBackDutyDateTo())
                        .withChargeDate(offence.getChargeDate())
                        .withArrestDate(offence.getArrestDate())
                        .withOffenceCode(offence.getOffenceDetails().getCjsOffenceCode())
                        .withOffenceCommittedDate(offence.getOffenceDetails().getOffenceCommittedDate())
                        .withOffenceCommittedEndDate(offence.getOffenceDetails().getOffenceCommittedEndDate())
                        .withOffenceDateCode(parseInt(offence.getOffenceDetails().getOffenceDateCode().toString()))
                        .withOffenceLocation(offence.getOffenceDetails().getOffenceLocation())
                        .withOffenceSequenceNumber(offence.getOffenceDetails().getOffenceSequenceNo())
                        .withOffenceWording(offence.getOffenceDetails().getOffenceWording())
                        .withOffenceWordingWelsh(offence.getOffenceDetails().getOffenceWordingWelsh())
                        .withStatementOfFacts(offence.getStatementOfFacts())
                        .withStatementOfFactsWelsh(offence.getStatementOfFactsWelsh())
                        .withAlcoholRelatedOffence(buildAlcoholRelatedOffence(offence))
                        .withAppliedCompensation(ofNullable(offence.getOffenceDetails().getProsecutorCompensation()).map(BigDecimal::new).orElse(null))
                        .withVehicleMake(offence.getOffenceDetails().getVehicleMake())
                        .withVehicleRegistrationMark(offence.getOffenceDetails().getVehicleRegistrationMark())
                        .build()
                )
                .collect(toList());
    }

    private AlcoholRelatedOffence buildAlcoholRelatedOffence(final Offence offence) {
        final uk.gov.moj.cpp.staging.prosecutors.json.schemas.AlcoholRelatedOffence alcoholRelatedOffence = offence.getOffenceDetails().getAlcoholRelatedOffence();

        if (alcoholRelatedOffence != null) {
            return AlcoholRelatedOffence.alcoholRelatedOffence()
                    .withAlcoholLevelAmount(alcoholRelatedOffence.getAlcoholOrDrugLevelAmount() != null ? alcoholRelatedOffence.getAlcoholOrDrugLevelAmount().intValue() : null)
                    .withAlcoholLevelMethod(alcoholRelatedOffence.getAlcoholOrDrugLevelMethod())
                    .build();
        }

        return null;
    }
}
