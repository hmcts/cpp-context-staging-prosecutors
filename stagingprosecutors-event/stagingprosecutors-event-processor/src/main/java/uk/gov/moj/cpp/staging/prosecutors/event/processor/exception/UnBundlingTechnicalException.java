package uk.gov.moj.cpp.staging.prosecutors.event.processor.exception;

public class UnBundlingTechnicalException extends RuntimeException {

    public UnBundlingTechnicalException(String message, Throwable throwable) {
        super(message, throwable);
    }
}