package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.Collections.singletonList;

import java.util.List;

public final class RuleConstants {

    private static final String CPPI_CONSUMERS = "CPPI Consumers";


    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getQuerySubmissionGroups() {
        return singletonList(CPPI_CONSUMERS);
    }
}

