package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class QuerySubmissionV2IT {

    @BeforeEach
    public void setUp() {
        new WiremockUtils();
    }

    @Test
    public void shouldReturn404WhenSubmissionNotFound() {
        try (Response response = StagingProsecutors.pollQuerySubmissionV2(randomUUID(), OUCODE)) {
            assertThat(response.getStatus(), is(HttpStatus.SC_NOT_FOUND));
        }
    }

}
