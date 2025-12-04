package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionQueryApiTest {

    @InjectMocks
    private SubmissionQueryApi api;

    @Mock
    private SubmissionQueryView SubmissionQueryView;

    @Mock
    private JsonEnvelope expectedEnvelope;

    @Mock
    private JsonObject expectedJsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Test
    public void queriesForSubmissionV2() {
        final String submissionId = UUID.randomUUID().toString();
        final String ouCode = "ouCode";
        JsonEnvelope envelope = createEnvelope("hmcts.cjs.submission.v2",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .add("oucode", ouCode)
                        .build());

        when(SubmissionQueryView.querySubmissionV2(envelope.payloadAsJsonObject())).thenReturn(expectedJsonObject);

        api.querySubmissionV2(envelope);
        verify(SubmissionQueryView).querySubmissionV2(jsonObjectArgumentCaptor.capture());
        verifyPayload(submissionId, ouCode);
    }

    @Test
    public void cpsQueriesForSubmissionV1() {
        final String submissionId = UUID.randomUUID().toString();
        JsonEnvelope envelope = createEnvelope("hmcts.cps.submission.v1",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .build()
        );

        api.cpsQuerySubmissionV1(envelope);
        verify(SubmissionQueryView).cpsQuerySubmissionV1(jsonEnvelopeArgumentCaptor.capture());
        verifyEnvelopeForCps("hmcts.cps.query.submission.v1", submissionId);
    }

    @Test
    public void queriesForSJPSubmissionV2() {
        final String submissionId = UUID.randomUUID().toString();
        final String ouCode = "ouCode";
        JsonEnvelope envelope = createEnvelope("hmcts.cjs.submission.v2",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .add("oucode", ouCode)
                        .build()
        );

        api.querySubmissionSjpV2(envelope);
        verify(SubmissionQueryView).querySubmissionSjpV2(jsonEnvelopeArgumentCaptor.capture());
        verifyEnvelope("hmcts.cjs.query.sjp.submission.v2", submissionId, ouCode);
    }


    @Test
    public void queriesForSJPSubmissionV2withV3ResponseType() {
        final String submissionId = UUID.randomUUID().toString();
        final String ouCode = "ouCode";
        JsonEnvelope envelope = createEnvelope("hmcts.cjs.submission.v3",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .add("oucode", ouCode)
                        .build()
        );

        api.querySubmissionV3(envelope);
        verify(SubmissionQueryView).querySubmissionV3(jsonEnvelopeArgumentCaptor.capture());
        verifyEnvelope("hmcts.cjs.query.submission.v3", submissionId, ouCode);
    }

    @Test
    public void queriesForSJPSubmissionV2withSjpV3ResponseType() {
        final String submissionId = UUID.randomUUID().toString();
        final String ouCode = "ouCode";
        JsonEnvelope envelope = createEnvelope("hmcts.cjs.sjp.submission.v3",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .add("oucode", ouCode)
                        .build()
        );

        api.querySubmissionSjpV3(envelope);
        verify(SubmissionQueryView).querySubmissionSjpV3(jsonEnvelopeArgumentCaptor.capture());
        verifyEnvelope("hmcts.cjs.query.sjp.submission.v3", submissionId, ouCode);
    }

    private void verifyPayload(final String submissionId, final String ouCode) {
        final JsonObject jsonObject = jsonObjectArgumentCaptor.getValue();
        assertThat(jsonObject.getString("submissionId"), is(submissionId));
        assertThat(jsonObject.getString("oucode"), is(ouCode));
    }

    private void verifyEnvelope(final String name, final String submissionId, final String ouCode) {
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is(name));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("submissionId"), is(submissionId));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("oucode"), is(ouCode));
    }

    private void verifyEnvelopeForCps(final String name, final String submissionId) {
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is(name));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("submissionId"), is(submissionId));
    }

    @Test
    public void queriesForSubmission() {
        final String submissionId = UUID.randomUUID().toString();
        JsonEnvelope envelope = createEnvelope("hmcts.cjs.submission",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .build()
        );

        api.querySubmission(envelope);
        verify(SubmissionQueryView).querySubmission(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("hmcts.cjs.query.submission"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("submissionId"), is(submissionId));
    }

    @Test
    public void validatesSubmissionIdAsUUID() {

        JsonEnvelope envelope = createEnvelope("stagingprosecutors.submission",
                createObjectBuilder()
                        .add("submissionId", "abc")
                        .build()
        );

        final BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> api.querySubmission(envelope));

        assertThat(badRequestException.getMessage(), is("Specified string abc, is not valid UUID"));

    }
}