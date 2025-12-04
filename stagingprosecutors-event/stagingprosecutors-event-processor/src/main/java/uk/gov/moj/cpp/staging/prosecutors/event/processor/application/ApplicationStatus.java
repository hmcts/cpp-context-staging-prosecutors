package uk.gov.moj.cpp.staging.prosecutors.event.processor.application;

import java.util.Optional;

public enum ApplicationStatus {
  CREATED("CREATED"),

  ERROR("ERROR");

  private final String value;

  ApplicationStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<ApplicationStatus> valueFor(final String value) {
    if(CREATED.value.equals(value)) { return Optional.of(CREATED); };
    if(ERROR.value.equals(value)) { return Optional.of(ERROR); };
    return Optional.empty();
  }
}
