package uk.gov.moj.cpp.staging.prosecutors.accesscontrol;

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
 * J17/J25 parity characterisation (J17 golden master), SP-M01 / BC-20.
 *
 * Deny assertions in the access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the COMMAND_API kbase loads.
 */
class CommandApiRuleCountParityTest {

    @Test
    void spM01_commandApiKbaseShouldLoadAllSeventeenAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("COMMAND_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Command - Rule for submitting a summons prosecution v2",
                "Command - Rule for submitting a charge prosecution v2",
                "Command - Rule for submitting a requisition prosecution v2",
                "Command - Rule for submitting an sjp prosecution v2",
                "Command - Rule for submitting a material v2",
                "Command - Rule for submitting a cps material v1",
                "Command - Rule for submitting a cps material with xml payload",
                "Command - Rule for submitting an sjp prosecution",
                "Command - Rule for submitting a material",
                "Command - Rule for submitting a material with ptiurn",
                "Command - Rule for submitting a material (case defendant / application defendant)",
                "Command - Rule for submit application",
                "Command - Rule for submitting serve pet form",
                "Command - Rule for submitting serve PTPH form",
                "Command - Rule for submitting serve BCM form",
                "Command - Rule for submitting serve COTR form",
                "Command - Rule for submitting update COTR form"));
    }
}
