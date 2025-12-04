package uk.gov.moj.cpp.staging.prosecutors;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createActualPayload;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createCourtApplicationSubject;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createCpsProsecutionCaseSubject;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createExhibit;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createExpectedPayloadWithCourtApplicationSubject;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createExpectedPayloadWithProsecutionCaseSubject;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createProsecutionCaseSubject;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createTags;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createWitnessStatement;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.createWitnessStatementExpected;
import static uk.gov.moj.cpp.staging.prosecutors.helper.StagingProsecutorsHelper.givenPayload;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCommandAPIV3Test {

    @Mock
    private Sender sender;

    @Mock
    private UUIDProducer uuidProducer;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @InjectMocks
    private StagingProsecutorsCommandAPIV3 stagingProsecutorsCommandAPIV3;

    @Captor
    private ArgumentCaptor<Envelope> materialEnvelopeCaptor;

    @Mock
    StringToJsonObjectConverter stringToJsonObjectConverter;

    private static final UUID SUBMISSION_ID = randomUUID();

    @Test
    public void shouldHandleSubmitMaterialWithJsonPayloadWithProsecutionCaseSubject() {

        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final JsonObject payload = givenPayload("json/stagingprosecutors.submit-material-v3-with-prosecution-case-subject.json");

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        final JsonObject expectedPayload = givenPayload("json/expected/stagingprosecutors.submit-material-v3-with-prosecution-case-subject.json");

        final JsonObject expectedPayloadWithSubmissionId = JsonObjects.createObjectBuilder(expectedPayload).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayloadWithSubmissionId));
    }

    @Test
    public void shouldHandleSubmitMaterialWithStringPayloadWithProsecutionCaseSubject() {
        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final UUID material = randomUUID();
        final UUID defendantId = randomUUID();
        final String testString = randomAlphabetic(10);

        final JsonObjectBuilder builder = createActualPayload();
        builder.add("prosecutionCaseSubject", createProsecutionCaseSubject(defendantId, testString).toString())
                .add("material", material.toString())
                .add("ouCode", "ouCode");
        final JsonObject payload = builder.build();

        when(stringToJsonObjectConverter.convert(any())).thenReturn(createProsecutionCaseSubject(defendantId, testString));

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        final JsonObject payload1 = createExpectedPayloadWithProsecutionCaseSubject(material, defendantId, testString).build();
        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(payload1).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayload));
    }

    @Test
    public void shouldHandleSubmitMaterialWithStringPayloadWithProsecutionCaseSubjectWitnessTagsAndExhibit() {

        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final UUID material = randomUUID();
        final UUID defendantId = randomUUID();
        final String testString = randomAlphabetic(10);

        final JsonObjectBuilder builder = createActualPayload();
        builder.add("prosecutionCaseSubject", createProsecutionCaseSubject(defendantId, testString).toString())
                .add("material", material.toString())
                .add("tag", createTags().build().toString())
                .add("witnessStatement", createWitnessStatement().toString())
                .add("exhibit", createExhibit().toString())
                .add("ouCode", "ouCode");

        final JsonObject payload = builder.build();

        when(stringToJsonObjectConverter.convert(payload.getString("prosecutionCaseSubject"))).thenReturn(createProsecutionCaseSubject(defendantId, testString));
        when(stringToJsonObjectConverter.convert(payload.getString("exhibit"))).thenReturn(createExhibit());
        when(stringToJsonObjectConverter.convert(payload.getString("witnessStatement"))).thenReturn(createWitnessStatement());

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        JsonObjectBuilder builder1 = createExpectedPayloadWithProsecutionCaseSubject(material, defendantId, testString);
        builder1.add("tag", createTags().build())
                .add("witnessStatement", createWitnessStatementExpected())
                .add("exhibit", createExhibit());

        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(builder1.build()).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayload));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadFailsSchemaValidation() {

        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        final JsonObject invalidPayload = createObjectBuilder()
                .add("material", "INVALID_UUID")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", invalidPayload);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadContainInvalidTitle() {

        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        final UUID material = randomUUID();
        final UUID defendantId = randomUUID();
        final String testString = randomAlphabetic(10);

        final JsonObjectBuilder builder = createActualPayload();
        builder.add("prosecutionCaseSubject", createCpsProsecutionCaseSubject(defendantId, testString).toString())
                .add("material", material.toString())
                .add("tag", createTags().build().toString())
                .add("witnessStatement", createWitnessStatement().toString())
                .add("exhibit", createExhibit().toString())
                .add("ouCode", "ouCode");

        final JsonObject payload = builder.build();

        when(stringToJsonObjectConverter.convert(payload.getString("prosecutionCaseSubject"))).thenReturn(createCpsProsecutionCaseSubject(defendantId, testString));
        when(stringToJsonObjectConverter.convert(payload.getString("exhibit"))).thenReturn(createExhibit());
        when(stringToJsonObjectConverter.convert(payload.getString("witnessStatement"))).thenReturn(createWitnessStatement());

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhileSubmitMaterialWithJsonPayloadWithInvalidTitle() {
        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        final JsonObject payload = givenPayload("json/stagingprosecutors.submit-material-v3-invalid-title.json");

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope));
    }

    @Test
    public void shouldHandleSubmitMaterialWithStringPayloadWithCourtApplicationSubject() {
        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final UUID material = randomUUID();
        final UUID courtApplicationId = randomUUID();

        final JsonObjectBuilder builder = createActualPayload();
        builder.add("material", material.toString())
                .add("courtApplicationSubject", createCourtApplicationSubject(courtApplicationId).toString());

        final JsonObject payload = builder.build();

        when(stringToJsonObjectConverter.convert(any())).thenReturn(createCourtApplicationSubject(courtApplicationId));

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        final JsonObject payload1 = createExpectedPayloadWithCourtApplicationSubject(material, courtApplicationId).build();
        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(payload1).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayload));
    }

    @Test
    public void shouldHandleSubmitMaterialWithJsonPayloadWithCourtApplicationSubject() {
        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final JsonObject payload = givenPayload("json/stagingprosecutors.submit-material-v3-with-court-application-subject.json");

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(payload).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayload));
    }

    @Test
    public void shouldHandleSubmitMaterialWithStringPayloadWithCourtApplicationSubjectWitnessTagsAndExhibit() {
        stagingProsecutorsCommandAPIV3.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final UUID material = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonObjectBuilder builder = createActualPayload();
        builder.add("courtApplicationSubject", createCourtApplicationSubject(applicationId).toString())
                .add("material", material.toString())
                .add("tag", createTags().build().toString())
                .add("witnessStatement", createWitnessStatement().toString())
                .add("exhibit", createExhibit().toString());

        final JsonObject payload = builder.build();

        when(stringToJsonObjectConverter.convert(payload.getString("courtApplicationSubject"))).thenReturn(createCourtApplicationSubject(applicationId));
        when(stringToJsonObjectConverter.convert(payload.getString("exhibit"))).thenReturn(createExhibit());
        when(stringToJsonObjectConverter.convert(payload.getString("witnessStatement"))).thenReturn(createWitnessStatement());

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-v3", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV3.submitMaterial(requestEnvelope);
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;

        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), is(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));
        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material-v3"));

        JsonObjectBuilder builder1 = createExpectedPayloadWithCourtApplicationSubject(material, applicationId);
        builder1.add("tag", createTags().build())
                .add("witnessStatement", createWitnessStatementExpected())
                .add("exhibit", createExhibit());

        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(builder1.build()).add("submissionId", SUBMISSION_ID.toString()).build();

        assertThat(sentEnvelope.payload(), equalTo(expectedPayload));
    }
}