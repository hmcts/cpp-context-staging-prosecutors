package uk.gov.moj.cpp.staging.prosecutors.event.processor.util;

public class InvalidCaseUrnProvided extends RuntimeException {
    public InvalidCaseUrnProvided(final String message) {
        super(message);
    }
}
