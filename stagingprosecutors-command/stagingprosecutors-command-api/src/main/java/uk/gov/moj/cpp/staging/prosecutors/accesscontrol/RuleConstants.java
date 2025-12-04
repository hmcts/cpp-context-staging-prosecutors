package uk.gov.moj.cpp.staging.prosecutors.accesscontrol;

import static java.util.Collections.singletonList;

import java.util.List;

public final class RuleConstants {

    private static final String CPPI_CONSUMERS = "CPPI Consumers";


    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getInitiateProsecutionGroup() {
        return singletonList(CPPI_CONSUMERS);
    }
}

