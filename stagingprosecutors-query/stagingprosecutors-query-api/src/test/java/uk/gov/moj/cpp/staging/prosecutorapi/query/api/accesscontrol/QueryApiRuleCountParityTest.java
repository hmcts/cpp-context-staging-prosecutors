package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

/**
 * J17/J25 parity characterisation (J17 golden master), SP-M02 / BC-20.
 *
 * Deny assertions in the access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the QUERY_API kbase loads.
 */
class QueryApiRuleCountParityTest {

    @Test
    void spM02_queryApiKbaseShouldLoadAllEightAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("QUERY_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Query - Rule for getting submission",
                "Query - Rule for getting submission for nonsjp v2 endpoint",
                "Query - Rule for getting submission for sjp v2 endpoint",
                "Query - Rule for getting submission from cps channel for nonsjp v1 endpoint",
                "Query - Rule for for the API: Gets the results for the given date range and organisational unit code",
                "Query - Rule for getting results with full verdict object for the v2 endpoint",
                "Query - Rule for getting submission for sjp v2 endpoint and v3 response type",
                "Query - Rule for getting submission for non sjp v2 endpoint and v3 response type"));
    }
}
