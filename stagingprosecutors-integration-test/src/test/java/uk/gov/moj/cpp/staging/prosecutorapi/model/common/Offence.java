package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import uk.gov.justice.services.common.converter.LocalDates;

import java.time.LocalDate;

import lombok.Builder;

@Builder
public class Offence {

    @Builder.Default
    public String cjsOffenceCode = "CA03010";

    @Builder.Default
    public Integer offenceSequenceNo = 1;

    @Builder.Default
    public String offenceCommittedDate = LocalDates.to(LocalDate.of(2018, 6, 20));

    @Builder.Default
    public String offenceCommittedEndDate = LocalDates.to(LocalDate.of(2018, 6, 21));

    @Builder.Default
    public String chargeDate = LocalDates.to(LocalDate.of(2018, 7, 10));

    @Builder.Default
    public int offenceDateCode = 1;

    @Builder.Default
    public String offenceLocation = "Croydon";

    @Builder.Default
    public String offenceWording = "Prosecution charge wording";

    @Builder.Default
    public String offenceWordingWelsh = "Prosecution charge wording in Welsh";

    @Builder.Default
    public String statementOfFacts = "Prosecution statements of facts";

    @Builder.Default
    public String statementOfFactsWelsh = "Prosecution statements of facts in Welsh";

    @Builder.Default
    public String prosecutorCompensation = "15.30";

    @Builder.Default
    public String backDuty = "340.30";

    @Builder.Default
    public String backDutyDateFrom = "2018-04-30";

    @Builder.Default
    public String backDutyDateTo = "2018-05-30";

    @Builder.Default
    public String vehicleMake = "Ford";

    @Builder.Default
    public String vehicleRegistrationMark = "AB11 ABC";

    @Builder.Default
    public String additionalProperty = null;

}
