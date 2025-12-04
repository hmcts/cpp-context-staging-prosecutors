package uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3;

import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.queryclient.Query;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.queryclient.QueryPoller;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Query(URI = "/v2/submissions/{submissionId}", contentType = "application/vnd.hmcts.cjs.submission.v3+json")
public class Submission {

    private final UUID submissionId;

    private final String submissionStatus;

    private final List<Problem> errors;

    private final List<Problem> warnings;

    private final String type;

    private final ZonedDateTime receivedAt;

    private final ZonedDateTime completedAt;

    @JsonCreator
    public Submission(
            @JsonProperty("id") final UUID submissionId,
            @JsonProperty("status") final String submissionStatus,
            @JsonProperty("errors") final List<Problem> errors,
            @JsonProperty("warnings") final List<Problem> warnings,
            @JsonProperty("type") final String type,
            @JsonProperty("receivedAt") final ZonedDateTime receivedAt,
            @JsonProperty("completedAt") final ZonedDateTime completedAt) {

        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
        this.errors = errors;
        this.warnings = warnings;
        this.type = type;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
    }

    public static QueryPoller<Submission> poller() {
        return new QueryPoller<>(Submission.class);
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public List<Problem> getErrors() {
        return errors;
    }

    public List<Problem> getWarnings() {
        return warnings;
    }

    public String getType() {
        return type;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }
}
