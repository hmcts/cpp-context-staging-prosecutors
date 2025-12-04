package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class QuerySubmissionTest extends BaseDroolsAccessControlTest {

    private static final String STAGING_PROSECUTORS_QUERY_SUBMISSION = "hmcts.cjs.submission";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QuerySubmissionTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToQuerySubmission() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SUBMISSION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQuerySubmissionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQuerySubmission() {
        final Action action = createActionFor(STAGING_PROSECUTORS_QUERY_SUBMISSION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }
}
