package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol.RuleConstants.getQuerySubmissionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class CaseResultsApiV2AccessControlTest extends BaseDroolsAccessControlTest {

    private static final String GET_RESULTS_V2 = "hmcts.results.v2";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public CaseResultsApiV2AccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryCaseResultsV2Api() {
        final Action action = createActionFor(GET_RESULTS_V2);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQuerySubmissionGroups()))
                .willReturn(true);
        final org.kie.api.runtime.ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQueryCaseResultsV2Api() {
        final Action action = createActionFor(GET_RESULTS_V2);
        final org.kie.api.runtime.ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
