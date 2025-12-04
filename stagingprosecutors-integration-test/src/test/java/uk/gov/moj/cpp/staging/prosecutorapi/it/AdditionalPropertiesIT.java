package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClient;
import uk.gov.moj.cpp.staging.prosecutorapi.model.command.SjpProsecutionSubmissionClientV2;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Address;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Defendant;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Offence;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Person;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutorapi.model.common.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.util.Optional;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdditionalPropertiesIT {

    private static final WiremockUtils wiremockUtils = new WiremockUtils();

    @BeforeEach
    public void setup() {
        wiremockUtils.stubIdMapperRecordingNewAssociation();
    }

    @Test
    public void checkIfAdditionalPropertiesAreNotPersisted() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .additionalProperty("additionalPropertyValue")
                .build();

        final SjpProsecutionSubmissionClient sjpProsecution = SjpProsecutionSubmissionClient.builder()
                .additionalProperty("additionalPropertyValue")
                .defendant(getDefendantWithAdditionalProperties())
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .build();


        final Optional<Response> submitSjpProsecutionResponse = sjpProsecution.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
        sjpProsecution.caseReceivedHandler = this::validateNoAdditionalPropertiesDataInTheEvent;
    }

    @Test
    public void checkIfAdditionalPropertiesAreNotPersistedV2() {
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = ProsecutionSubmissionDetails.builder()
                .prosecutingAuthority(OUCODE)
                .additionalProperty("additionalPropertyValue")
                .build();

        final SjpProsecutionSubmissionClientV2 sjpProsecutionV2 = SjpProsecutionSubmissionClientV2.builder()
                .additionalProperty("additionalPropertyValue")
                .defendant(getDefendantWithAdditionalProperties())
                .prosecutionSubmissionDetails(prosecutionSubmissionDetails)
                .build();

        final Optional<Response> submitSjpProsecutionResponse = sjpProsecutionV2.getExecutor().executeSync();
        assertThat(submitSjpProsecutionResponse.get().getStatus(), equalTo(202));
        sjpProsecutionV2.caseReceivedHandler = this::validateNoAdditionalPropertiesDataInTheEvent;

    }

    private Defendant getDefendantWithAdditionalProperties() {
        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder()
                .additionalProperty("additionalPropertyValue")
                .build();

        final Address address = Address.builder()
                .build();

        final ContactDetails contactDetails = ContactDetails.builder()
                .build();

        final Person person = Person.builder()
                .additionalProperty("additionalPropertyValue")
                .selfDefinedInformation(selfDefinedInformation)
                .address(address)
                .contactDetails(contactDetails)
                .build();

        final Offence offence = Offence.builder()
                .additionalProperty("additionalPropertyValue")
                .build();

        return Defendant.builder()
                .additionalProperty("additionalPropertyValue")
                .defendantPerson(person)
                .organisation(null)
                .offences(new Offence[]{offence})
                .build();
    }

    private void validateNoAdditionalPropertiesDataInTheEvent(final Object eventEnvelope) {
        final JsonObject eventPayload = ((JsonEnvelope) eventEnvelope).payloadAsJsonObject();

        assertThat(eventPayload, payloadIsJson(
                withoutJsonPath(
                        "$..additionalProperties"
                )
        ));
    }
}
