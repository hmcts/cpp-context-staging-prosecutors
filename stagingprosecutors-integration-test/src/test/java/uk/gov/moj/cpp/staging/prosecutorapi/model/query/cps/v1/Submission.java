package uk.gov.moj.cpp.staging.prosecutorapi.model.query.cps.v1;

import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.queryclient.Query;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.queryclient.QueryPoller;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Query(URI = "/v1/submissions/cps/{submissionId}", contentType = "application/vnd.hmcts.cps.submission.v1+json")
public class Submission {

    private final UUID submissionId;

    private final String submissionStatus;

    private final List<Problem> errors;

    private final List<Problem> caseErrors;

    private final List<DefendantProblem> DefendantErrors;

    private final List<Problem> warnings;

    private final List<Problem> caseWarnings;

    private final List<DefendantProblem> defendantWarnings;

    private final String type;

    private final ZonedDateTime receivedAt;

    private final ZonedDateTime completedAt;

    @JsonCreator
    public Submission(
            @JsonProperty("id") final UUID submissionId,
            @JsonProperty("status") final String submissionStatus,
            @JsonProperty("errors") final List<Problem> errors,
            @JsonProperty("caseErrors") final List<Problem> caseErrors,
            @JsonProperty("defendantErrors") final List<DefendantProblem> defendantErrors,
            @JsonProperty("warnings") final List<Problem> warnings,
            @JsonProperty("caseWarnings") final List<Problem> caseWarnings,
            @JsonProperty("defendantWarnings") final List<DefendantProblem> defendantWarnings,
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
        this.caseErrors = caseErrors;
        this.caseWarnings = caseWarnings;
        this.DefendantErrors = defendantErrors;
        this.defendantWarnings = defendantWarnings;
    }

    public static QueryPoller<Submission> poller() {
        return new QueryPoller<>(Submission.class);
    }

}
