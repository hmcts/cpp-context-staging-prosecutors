package uk.gov.moj.cpp.staging.prosecutors.uuid;

import static java.util.UUID.randomUUID;

import java.util.UUID;

public class DefaultUUIDProducer implements UUIDProducer {

    @Override
    public UUID generateUUID() {
        return randomUUID();
    }
}
