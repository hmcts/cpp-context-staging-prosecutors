package uk.gov.moj.cpp.staging.prosecutors.event.processor.application;

import java.time.LocalDate;

public class CaseDefendantIndividual {
  private final String asn;

  private final LocalDate dateOfBirth;

  private final String forename;

  private final String forename2;

  private final String forename3;

  private final String prosecutorDefendantId;

  private final String surname;

  private final String title;

  public CaseDefendantIndividual(final String asn, final LocalDate dateOfBirth, final String forename, final String forename2, final String forename3, final String prosecutorDefendantId, final String surname, final String title) {
    this.asn = asn;
    this.dateOfBirth = dateOfBirth;
    this.forename = forename;
    this.forename2 = forename2;
    this.forename3 = forename3;
    this.prosecutorDefendantId = prosecutorDefendantId;
    this.surname = surname;
    this.title = title;
  }

  public String getAsn() {
    return asn;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public String getForename() {
    return forename;
  }

  public String getForename2() {
    return forename2;
  }

  public String getForename3() {
    return forename3;
  }

  public String getProsecutorDefendantId() {
    return prosecutorDefendantId;
  }

  public String getSurname() {
    return surname;
  }

  public String getTitle() {
    return title;
  }

  public static Builder caseDefendantIndividual() {
    return new Builder();
  }

  public static class Builder {
    private String asn;

    private LocalDate dateOfBirth;

    private String forename;

    private String forename2;

    private String forename3;

    private String prosecutorDefendantId;

    private String surname;

    private String title;

    public Builder withAsn(final String asn) {
      this.asn = asn;
      return this;
    }

    public Builder withDateOfBirth(final LocalDate dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withForename(final String forename) {
      this.forename = forename;
      return this;
    }


    public Builder withForename2(final String forename2) {
      this.forename2 = forename2;
      return this;
    }


    public Builder withForename3(final String forename3) {
      this.forename3 = forename3;
      return this;
    }

    public Builder withProsecutorDefendantId(final String prosecutorDefendantId) {
      this.prosecutorDefendantId = prosecutorDefendantId;
      return this;
    }

    public Builder withSurname(final String surname) {
      this.surname = surname;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withValuesFrom(final CaseDefendantIndividual caseDefendantIndividual) {
      this.asn = caseDefendantIndividual.getAsn();
      this.dateOfBirth = caseDefendantIndividual.getDateOfBirth();
      this.forename = caseDefendantIndividual.getForename();
      this.forename2 = caseDefendantIndividual.getForename2();
      this.forename3 = caseDefendantIndividual.getForename3();
      this.prosecutorDefendantId = caseDefendantIndividual.getProsecutorDefendantId();
      this.surname = caseDefendantIndividual.getSurname();
      this.title = caseDefendantIndividual.getTitle();
      return this;
    }

    public CaseDefendantIndividual build() {
      return new CaseDefendantIndividual(asn, dateOfBirth, forename, forename2, forename3,prosecutorDefendantId,surname, title);
    }
  }
}
