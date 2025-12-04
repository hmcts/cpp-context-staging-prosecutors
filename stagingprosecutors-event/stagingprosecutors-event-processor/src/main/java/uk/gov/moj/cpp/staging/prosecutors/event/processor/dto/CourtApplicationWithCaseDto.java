package uk.gov.moj.cpp.staging.prosecutors.event.processor.dto;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.List;
import java.util.Objects;

public class CourtApplicationWithCaseDto {


    private final BoxHearingRequest boxHearing;
    private final CourtApplication courtApplication;
    private final CourtHearingRequest courtHearing;
    private final Boolean summonsApprovalRequired;
    private List<ProsecutionCase> prosecutionCases;

    public CourtApplicationWithCaseDto(final BoxHearingRequest boxHearing, final CourtApplication courtApplication, final CourtHearingRequest courtHearing, final Boolean summonsApprovalRequired, final List<ProsecutionCase> prosecutionCases) {
        this.boxHearing = boxHearing;
        this.courtApplication = courtApplication;
        this.courtHearing = courtHearing;
        this.summonsApprovalRequired = summonsApprovalRequired;
        this.prosecutionCases = prosecutionCases;
    }

    public BoxHearingRequest getBoxHearing() {
        return boxHearing;
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }

    public CourtHearingRequest getCourtHearing() {
        return courtHearing;
    }

    public Boolean getSummonsApprovalRequired() {
        return summonsApprovalRequired;
    }

    public List<ProsecutionCase> getProsecutionCases() {
        return prosecutionCases;
    }

    public void setProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
        this.prosecutionCases = prosecutionCases;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CourtApplicationWithCaseDto that = (CourtApplicationWithCaseDto) o;
        return Objects.equals(boxHearing, that.boxHearing) && Objects.equals(courtApplication, that.courtApplication) && Objects.equals(courtHearing, that.courtHearing) && Objects.equals(summonsApprovalRequired, that.summonsApprovalRequired) && Objects.equals(prosecutionCases, that.prosecutionCases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boxHearing, courtApplication, courtHearing, summonsApprovalRequired, prosecutionCases);
    }

    @Override
    public String toString() {
        return "CourtApplicationWithCaseDto{" +
                "boxHearing=" + boxHearing +
                ", courtApplication=" + courtApplication +
                ", courtHearing=" + courtHearing +
                ", summonsApprovalRequired=" + summonsApprovalRequired +
                ", prosecutionCases=" + prosecutionCases +
                '}';
    }
}
