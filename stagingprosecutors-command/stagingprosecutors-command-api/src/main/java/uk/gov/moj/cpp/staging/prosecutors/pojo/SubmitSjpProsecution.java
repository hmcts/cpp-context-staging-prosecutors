package uk.gov.moj.cpp.staging.prosecutors.pojo;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;

import java.util.UUID;

@SuppressWarnings({"squid:S00121", "squid:S00122"})
public class SubmitSjpProsecution {

    private final SjpDefendant defendant;

    private final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails;

    private final UUID submissionId;

    public SubmitSjpProsecution(final SjpDefendant defendant, final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails, final UUID submissionId) {
        this.defendant = defendant;
        this.prosecutionSubmissionDetails = prosecutionSubmissionDetails;
        this.submissionId = submissionId;
    }

    public SjpDefendant getDefendant() {
        return defendant;
    }

    public SjpProsecutionSubmissionDetails getProsecutionSubmissionDetails() {
        return prosecutionSubmissionDetails;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public static Builder submitSjpProsecution() {
        return new SubmitSjpProsecution.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SubmitSjpProsecution that = (SubmitSjpProsecution) obj;

        return java.util.Objects.equals(this.defendant, that.defendant) &&
                java.util.Objects.equals(this.prosecutionSubmissionDetails, that.prosecutionSubmissionDetails) &&
                java.util.Objects.equals(this.submissionId, that.submissionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defendant, prosecutionSubmissionDetails, submissionId);
    }

    @Override
    public String toString() {
        return "SubmitSjpProsecution{" +
                "defendant='" + defendant + "'," +
                "prosecutionSubmissionDetails='" + prosecutionSubmissionDetails + "'," +
                "submissionId='" + submissionId + "'" +
                "}";
    }

    public static class Builder {
        private SjpDefendant defendant;

        private SjpProsecutionSubmissionDetails prosecutionSubmissionDetails;

        private UUID submissionId;

        public Builder withDefendant(final SjpDefendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public Builder withProsecutionSubmissionDetails(final SjpProsecutionSubmissionDetails prosecutionSubmissionDetails) {
            this.prosecutionSubmissionDetails = prosecutionSubmissionDetails;
            return this;
        }

        public Builder withSubmissionId(final UUID submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public SubmitSjpProsecution build() {
            return new SubmitSjpProsecution(defendant, prosecutionSubmissionDetails, submissionId);
        }

    }
}