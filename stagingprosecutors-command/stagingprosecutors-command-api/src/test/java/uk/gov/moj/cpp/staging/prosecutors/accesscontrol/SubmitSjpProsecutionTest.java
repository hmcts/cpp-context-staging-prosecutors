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

public class SubmitSjpProsecutionTest extends BaseDroolsAccessControlTest {

    private static final String HMCTS_CJS_SJP_PROSECUTION = "hmcts.cjs.sjp-prosecution";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SubmitSjpProsecutionTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
    
    @Test
    public void shouldAllowAuthorisedUserToSubmitSjpProsecution() {
        final Action action = createActionFor(HMCTS_CJS_SJP_PROSECUTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getInitiateProsecutionGroup()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToSubmitSjpProsecution() {
        final Action action = createActionFor(HMCTS_CJS_SJP_PROSECUTION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
