package uk.gov.moj.cpp.staging.prosecutorapi.model.query;

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
@Query(URI = "/v1/submissions/{submissionId}", contentType = "application/vnd.hmcts.cjs.submission+json")
public class Submission {

    UUID submissionId;

    String submissionStatus;

    List<Problem> errors;

    List<Problem> warnings;

    String type;

    ZonedDateTime receivedAt;

    ZonedDateTime completedAt;

    @JsonCreator
    public Submission(
            @JsonProperty("id") UUID submissionId,
            @JsonProperty("status") String submissionStatus,
            @JsonProperty("errors") List<Problem> errors,
            @JsonProperty("warnings") List<Problem> warnings,
            @JsonProperty("type") String type,
            @JsonProperty("receivedAt") ZonedDateTime receivedAt,
            @JsonProperty("completedAt") ZonedDateTime completedAt) {

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

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public String getType() {
        return type;
    }
}
