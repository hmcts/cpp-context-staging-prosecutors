package uk.gov.moj.cpp.staging.prosecutors.persistence.entity;


import uk.gov.moj.cpp.staging.prosecutors.persistence.converter.JsonArrayConverter;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonArray;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "submission")
@SuppressWarnings("squid:S2384")
public class Submission {

    @Column(name = "submission_id")
    @Id
    private UUID submissionId;

    @Column(name = "submission_status")
    private String submissionStatus;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "ou_code")
    private String ouCode;

    @Column(name = "errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray errors;

    @Column(name = "case_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray caseErrors;

    @Column(name = "defendant_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantErrors;

    @Column(name = "warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray warnings;

    @Column(name = "defendant_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantWarnings;

    @Column(name = "case_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray caseWarnings;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private SubmissionType type;

    @Column(name = "received_at", nullable = false)
    private ZonedDateTime receivedAt;

    @Column(name = "completed_at", nullable = true)
    private ZonedDateTime completedAt;

    @Column(name = "is_cps_case", nullable = true)
    private Boolean isCpsCase;

    @Column(name = "application_id")
    private UUID applicationId;


    public Submission() {
    }

    @SuppressWarnings("squid:S00107")
    public Submission(final UUID submissionId,
                      final String submissionStatus,
                      final String caseUrn,
                      final String ouCode,
                      final JsonArray errors,
                      final JsonArray warnings,
                      final SubmissionType type,
                      final ZonedDateTime receivedAt,
                      final Boolean isCpsCase,
                      final UUID applicationId) {

        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
        this.caseUrn = caseUrn;
        this.ouCode = ouCode;
        this.errors = errors;
        this.warnings = warnings;
        this.type = type;
        this.receivedAt = receivedAt;
        this.isCpsCase = isCpsCase;
        this.applicationId = applicationId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(final UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(final String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public String getOuCode() {
        return ouCode;
    }

    public void setOuCode(final String ouCode) {
        this.ouCode = ouCode;
    }

    public JsonArray getErrors() {
        return errors;
    }

    public void setErrors(final JsonArray errors) {
        this.errors = errors;
    }

    public JsonArray getWarnings() {
        return warnings;
    }

    public void setWarnings(final JsonArray warnings) {
        this.warnings = warnings;
    }

    public SubmissionType getType() {
        return type;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public JsonArray getDefendantErrors() {
        return defendantErrors;
    }

    public void setDefendantErrors(final JsonArray defendantErrors) {
        this.defendantErrors = defendantErrors;
    }

    public JsonArray getDefendantWarnings() {
        return defendantWarnings;
    }

    public void setDefendantWarnings(final JsonArray defendantWarnings) {
        this.defendantWarnings = defendantWarnings;
    }

    public JsonArray getCaseWarnings() {
        return caseWarnings;
    }

    public void setCaseWarnings(JsonArray caseWarnings) {
        this.caseWarnings = caseWarnings;
    }

    public JsonArray getCaseErrors() {
        return caseErrors;
    }

    public void setCaseErrors(final JsonArray caseErrors) {
        this.caseErrors = caseErrors;
    }

    public Boolean getCpsCase() { return isCpsCase; }

    public void setCpsCase(final Boolean cpsCase) { this.isCpsCase = cpsCase; }

    public void setType(final SubmissionType type) {
        this.type = type;
    }

    public void setReceivedAt(final ZonedDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isCpsCase(){
        if(this.isCpsCase == null) {
            return false;
        }

        return getCpsCase();
    }
}
