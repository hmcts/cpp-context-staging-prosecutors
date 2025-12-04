package uk.gov.moj.cpp.staging.prosecutors.uuid;

import java.util.UUID;

@FunctionalInterface
public interface UUIDProducer {
    UUID generateUUID();
}
