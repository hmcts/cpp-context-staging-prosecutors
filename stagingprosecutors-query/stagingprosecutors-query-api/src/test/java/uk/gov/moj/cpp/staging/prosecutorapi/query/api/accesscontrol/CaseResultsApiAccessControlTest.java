package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol.RuleConstants.getQuerySubmissionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class CaseResultsApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String GET_RESULTS = "hmcts.results.v1";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public CaseResultsApiAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryCaseResultsApi() {
        final Action action = createActionFor(GET_RESULTS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQuerySubmissionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToQueryCaseResultsApi() {
        final Action action = createActionFor(GET_RESULTS);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
