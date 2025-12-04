package uk.gov.moj.cpp.staging.prosecutors.event.processor.application;

import java.util.List;

public class ApplicationRequestNotification {
  private final String applicationId;

  private final String applicationStatus;

  private final List<CaseDefendantIndividual> caseDefendantIndividuals;

  private final List<CaseDefendantOrganisation> caseDefendantOrganisations;

  private final String validationErrorType;

  public ApplicationRequestNotification(final String applicationId, final String applicationStatus, final List<CaseDefendantIndividual> caseDefendantIndividuals, final List<CaseDefendantOrganisation> caseDefendantOrganisations, final String validationErrorType) {
    this.applicationId = applicationId;
    this.applicationStatus = applicationStatus;
    this.caseDefendantIndividuals = caseDefendantIndividuals;
    this.caseDefendantOrganisations = caseDefendantOrganisations;
    this.validationErrorType = validationErrorType;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public String getApplicationStatus() {
    return applicationStatus;
  }

  public List<CaseDefendantIndividual> getCaseDefendantIndividuals() {
    return caseDefendantIndividuals;
  }

  public List<CaseDefendantOrganisation> getCaseDefendantOrganisations() {
    return caseDefendantOrganisations;
  }

  public String getValidationErrorType() {
      return validationErrorType;
  }

  public static Builder applicationRequestNotification() {
    return new Builder();
  }

  public static class Builder {
    private String applicationId;

    private String applicationStatus;

    private List<CaseDefendantIndividual> caseDefendantIndividuals;

    private List<CaseDefendantOrganisation> caseDefendantOrganisations;

    private String validationErrorType;

    public Builder withApplicationId(final String applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withApplicationStatus(final String applicationStatus) {
      this.applicationStatus = applicationStatus;
      return this;
    }

    public Builder withCaseDefendantIndividuals(final List<CaseDefendantIndividual> caseDefendantIndividuals) {
      this.caseDefendantIndividuals = caseDefendantIndividuals;
      return this;
    }

    public Builder withCaseDefendantOrganisations(final List<CaseDefendantOrganisation> caseDefendantOrganisations) {
      this.caseDefendantOrganisations = caseDefendantOrganisations;
      return this;
    }

    public Builder withValidationErrorType(final String validationErrorType) {
      this.validationErrorType = validationErrorType;
      return this;
    }

    public Builder withValuesFrom(final ApplicationRequestNotification applicationRequestNotification) {
      this.applicationId = applicationRequestNotification.getApplicationId();
      this.applicationStatus = applicationRequestNotification.getApplicationStatus();
      this.caseDefendantIndividuals = applicationRequestNotification.getCaseDefendantIndividuals();
      this.caseDefendantOrganisations = applicationRequestNotification.getCaseDefendantOrganisations();
      this.validationErrorType = applicationRequestNotification.getValidationErrorType();
      return this;
    }

    public ApplicationRequestNotification build() {
      return new ApplicationRequestNotification(applicationId, applicationStatus, caseDefendantIndividuals, caseDefendantOrganisations, validationErrorType);
    }
  }
}
