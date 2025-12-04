package uk.gov.moj.cpp.staging.prosecutors.event.processor.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}