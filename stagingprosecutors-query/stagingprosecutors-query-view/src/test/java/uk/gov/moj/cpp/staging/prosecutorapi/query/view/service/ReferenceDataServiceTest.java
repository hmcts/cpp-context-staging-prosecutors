package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private static final String ouCode = "013000";
    private static final String REFERENCE_DATA_QUERY_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";

    @Test
    public void shouldGetProsecutorByOuCode() {

        final String prosecutorId = randomUUID().toString();

        final JsonObject prosecutorPayload = createObjectBuilder()
                .add("id", prosecutorId)
                .add("cpsFlag", true)
                .build();

        final Envelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName(REFERENCE_DATA_QUERY_PROSECUTOR_BY_OUCODE).build(), prosecutorPayload);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);

        final Optional<JsonObject> result = referenceDataService.getProsecutorByOuCode(ouCode);

        assertThat(result.get(), is(prosecutorPayload));
        assertThat(result.get().getBoolean("cpsFlag"), is(true));
    }
}
