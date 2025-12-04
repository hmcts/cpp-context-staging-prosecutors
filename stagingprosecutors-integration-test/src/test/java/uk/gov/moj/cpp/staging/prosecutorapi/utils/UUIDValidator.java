package uk.gov.moj.cpp.staging.prosecutorapi.utils;

import java.util.UUID;
import java.util.function.Function;

public class UUIDValidator {

    public static Function<String, Boolean> isValidUUID = (input) -> {
        try {
            UUID.fromString(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    };
}
