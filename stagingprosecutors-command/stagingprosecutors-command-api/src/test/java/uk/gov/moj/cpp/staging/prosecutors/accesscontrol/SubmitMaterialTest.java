package uk.gov.moj.cpp.staging.prosecutors.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.staging.prosecutors.accesscontrol.RuleConstants.getInitiateProsecutionGroup;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SubmitMaterialTest extends BaseDroolsAccessControlTest {

    private static final String STAGING_PROSECUTORS_SUBMIT_MATERIAL = "stagingprosecutors.submit-material";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SubmitMaterialTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToSubmitMaterial() {
        final Action action = createActionFor(STAGING_PROSECUTORS_SUBMIT_MATERIAL);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getInitiateProsecutionGroup()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToSubmitMaterial() {
        final Action action = createActionFor(STAGING_PROSECUTORS_SUBMIT_MATERIAL);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
