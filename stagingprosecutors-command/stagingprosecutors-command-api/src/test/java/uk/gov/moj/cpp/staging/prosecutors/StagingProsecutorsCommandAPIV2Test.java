package uk.gov.moj.cpp.staging.prosecutors;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeDefendant.chargeDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeOffence.chargeOffence;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeProsecutionSubmissionDetails.chargeProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionDefendant.requisitionDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionOffence.requisitionOffence;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionProsecutionSubmissionDetails.requisitionProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionHttpWithOucode.submitChargeProsecutionHttpWithOucode;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionWithSubmissionId.submitChargeProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionHttpWithOucode.submitRequisitionProsecutionHttpWithOucode;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionWithSubmissionId.submitRequisitionProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionHttpWithOucode.submitSummonsProsecutionHttpWithOucode;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionWithSubmissionId.submitSummonsProsecutionWithSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsDefendant.summonsDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsOffence.summonsOffence;
import static uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsProsecutionSubmissionDetails.summonsProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails.offenceDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails.sjpProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpWithOucode.submitSjpProsecutionHttpWithOucode;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.SubmissionQueryView;
import uk.gov.moj.cpp.staging.prosecutors.command.api.ChargeDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.api.RequisitionDefendant;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitChargeProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitRequisitionProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SubmitSummonsProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.command.api.SummonsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitSjpProsecutionV2Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpV2;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmitSjpProsecutionHttpWithOucode;
import uk.gov.moj.cpp.staging.prosecutors.pojo.SubmitSjpProsecution;
import uk.gov.moj.cpp.staging.prosecutors.service.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.validators.DefendantValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.OffenceValidator;
import uk.gov.moj.cpp.staging.prosecutors.validators.SubmitSjpProsecutionHttpV2Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCommandAPIV2Test {

    private static final UUID SUBMISSION_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String OUCODE = "oucode";
    private static final String OUCODE_UPPER_CASE = "OUCODE";
    private static final String INVALID_URN = " ";

    @Mock
    private Sender sender;

    @Mock
    private UUIDProducer uuidProducer;

    @Mock
    private SubmitSjpProsecutionV2Converter submitSjpProsecutionV2Converter;

    @Mock
    private SubmitSjpProsecutionHttpV2Validator submitSjpProsecutionHttpV2Validator;

    @Mock
    private OffenceValidator offenceValidator;

    @Mock
    private DefendantValidator defendantValidator;

    @Mock
    private JsonSchemaValidator jsonSchemaValidator;

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private SubmissionQueryView submissionQueryView;

    @InjectMocks
    private StagingProsecutorsCommandAPIV2 stagingProsecutorsCommandAPIV2;

    @Captor
    private ArgumentCaptor<Envelope<SubmitRequisitionProsecutionWithSubmissionId>> requisitionEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<SubmitChargeProsecutionWithSubmissionId>> chargeEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<SubmitSummonsProsecutionWithSubmissionId>> summonsEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<SubmitSjpProsecution>> sjpEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope> materialEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<List<OffenceDetails>> offenceDetailsArgumentCaptor;

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForSummonsCase() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitSummonsProsecutionHttpWithOucode payload = submitSummonsProsecutionHttpWithOucode()
                .withProsecutionSubmissionDetails(summonsProsecutionSubmissionDetails()
                        .withProsecutingAuthority("prosecutingAuthority")
                        .build())
                .withOucode(OUCODE)
                .build();
        final Envelope<SubmitSummonsProsecutionHttpWithOucode> originalEnvelope = createSubmitSummonsProsecutionEnvelope(payload);

        assertThrows(ForbiddenRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitSummonsProsecution(originalEnvelope));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForChargeCase() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitChargeProsecutionHttpWithOucode payload = submitChargeProsecutionHttpWithOucode()
                .withProsecutionSubmissionDetails(chargeProsecutionSubmissionDetails()
                        .withProsecutingAuthority("prosecutingAuthority")
                        .build())
                .withOucode(OUCODE)
                .build();
        final Envelope<SubmitChargeProsecutionHttpWithOucode> originalEnvelope = createSubmitChargeProsecutionEnvelope(payload);

        assertThrows(ForbiddenRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitChargeProsecution(originalEnvelope));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForRequisitionCase() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitRequisitionProsecutionHttpWithOucode payload = submitRequisitionProsecutionHttpWithOucode()
                .withProsecutionSubmissionDetails(requisitionProsecutionSubmissionDetails()
                        .withProsecutingAuthority("prosecutingAuthority")
                        .build())
                .withOucode(OUCODE)
                .build();
        final Envelope<SubmitRequisitionProsecutionHttpWithOucode> originalEnvelope = createSubmitRequisitionProsecutionEnvelope(payload);

        assertThrows(ForbiddenRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitRequisitionProsecution(originalEnvelope));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForSJPCase() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitSjpProsecutionHttpWithOucode payload = submitSjpProsecutionHttpWithOucode()
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails()
                        .withProsecutingAuthority("prosecutingAuthority")
                        .build())
                .withOucode(OUCODE)
                .build();
        final Envelope<SubmitSjpProsecutionHttpWithOucode> originalEnvelope = createSubmitSjpProsecutionEnvelope(payload);

        assertThrows(ForbiddenRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitSJPProsecution(originalEnvelope));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenOucodeMismatchForSubmittingMaterial() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", "EETFL01")
                .add("materialType", "SJPN")
                .add("defendantId", "DefendantA001")
                .add("oucode", OUCODE)
                .build();
        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", payload);

        assertThrows(ForbiddenRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitMaterial(requestEnvelope));
    }

    @Test
    public void handleSummonsProsecution() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitSummonsProsecutionHttpWithOucode payload = getSummonsProsecutionCase();
        final SubmitSummonsProsecutionWithSubmissionId convertedPayload =
                submitSummonsProsecutionWithSubmissionId()
                        .withDefendants(payload.getDefendants())
                        .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                        .withSubmissionId(SUBMISSION_ID)
                        .build();

        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));

        final Envelope<SubmitSummonsProsecutionHttpWithOucode> originalEnvelope = createSubmitSummonsProsecutionEnvelope(payload);
        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandAPIV2.submitSummonsProsecution(originalEnvelope);
        //then
        verify(offenceValidator, times(3)).validate(offenceDetailsArgumentCaptor.capture(), any(Map.class));

        final List<OffenceDetails> offenceDetailsList1 = asList(payload.getDefendants().get(0).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(0).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList2 = asList(payload.getDefendants().get(1).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(1).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList3 = asList(payload.getDefendants().get(2).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(2).getOffences().get(1).getOffenceDetails());

        final List<List<OffenceDetails>> expectedOffenceDetailsList = asList(offenceDetailsList1, offenceDetailsList2, offenceDetailsList3);
        final List<List<OffenceDetails>> actualOffenceDetailsList = offenceDetailsArgumentCaptor.getAllValues();

        assertEquals(expectedOffenceDetailsList, actualOffenceDetailsList);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(summonsEnvelopeCaptor.capture());

        final Envelope<SubmitSummonsProsecutionWithSubmissionId> sentEnvelope = summonsEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitSummonsProsecutionWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.summons-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    private SubmitSummonsProsecutionHttpWithOucode getSummonsProsecutionCase() {
        final List<OffenceDetails> offenceDetailsDuplicateSequenceNumbers = getOffenceDetails();
        final SummonsDefendant summonsDefendant1 = summonsDefendant()
                .withOffences(asList(
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(0))
                                .build(),
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(1))
                                .build()))
                .build();
        final SummonsDefendant summonsDefendant2 = summonsDefendant()
                .withOffences(asList(
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(2))
                                .build(),
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(3))
                                .build()))
                .build();
        final SummonsDefendant summonsDefendant3 = summonsDefendant()
                .withOffences(asList(
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(4))
                                .build(),
                        summonsOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(5))
                                .build()))
                .build();

        return submitSummonsProsecutionHttpWithOucode()
                .withDefendants(asList(
                        summonsDefendant1,
                        summonsDefendant2,
                        summonsDefendant3))
                .withProsecutionSubmissionDetails(summonsProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE_UPPER_CASE)
                        .build())
                .withOucode(OUCODE)
                .build();
    }

    @Test
    public void handleChargeProsecution() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";

        final SubmitChargeProsecutionHttpWithOucode payload = getChargeProsecutionCase();
        final SubmitChargeProsecutionWithSubmissionId convertedPayload =
                submitChargeProsecutionWithSubmissionId()
                        .withDefendants(payload.getDefendants())
                        .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                        .withSubmissionId(SUBMISSION_ID)
                        .build();

        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));
        final Envelope<SubmitChargeProsecutionHttpWithOucode> originalEnvelope = createSubmitChargeProsecutionEnvelope(payload);
        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandAPIV2.submitChargeProsecution(originalEnvelope);

        //then
        verify(offenceValidator, times(3)).validate(offenceDetailsArgumentCaptor.capture(), any(Map.class));

        final List<OffenceDetails> offenceDetailsList1 = asList(payload.getDefendants().get(0).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(0).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList2 = asList(payload.getDefendants().get(1).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(1).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList3 = asList(payload.getDefendants().get(2).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(2).getOffences().get(1).getOffenceDetails());

        final List<List<OffenceDetails>> expectedOffenceDetailsList = asList(offenceDetailsList1, offenceDetailsList2, offenceDetailsList3);
        final List<List<OffenceDetails>> actualOffenceDetailsList = offenceDetailsArgumentCaptor.getAllValues();

        assertEquals(expectedOffenceDetailsList, actualOffenceDetailsList);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(chargeEnvelopeCaptor.capture());

        final Envelope<SubmitChargeProsecutionWithSubmissionId> sentEnvelope = chargeEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitChargeProsecutionWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.charge-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    @Test
    public void handleChargeProsecutionWithInvalidUrn() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitChargeProsecutionHttpWithOucode payload = getChargeProsecutionCaseWithInvalidUrn();
        final SubmitChargeProsecutionWithSubmissionId convertedPayload =
                submitChargeProsecutionWithSubmissionId()
                        .withDefendants(payload.getDefendants())
                        .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                        .withSubmissionId(SUBMISSION_ID)
                        .build();

        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));
        final Envelope<SubmitChargeProsecutionHttpWithOucode> originalEnvelope = createSubmitChargeProsecutionEnvelope(payload);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandAPIV2.submitChargeProsecution(originalEnvelope);

        //then
        verify(offenceValidator, times(3)).validate(offenceDetailsArgumentCaptor.capture(), any(Map.class));

        final List<OffenceDetails> offenceDetailsList1 = asList(payload.getDefendants().get(0).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(0).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList2 = asList(payload.getDefendants().get(1).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(1).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList3 = asList(payload.getDefendants().get(2).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(2).getOffences().get(1).getOffenceDetails());

        final List<List<OffenceDetails>> expectedOffenceDetailsList = asList(offenceDetailsList1, offenceDetailsList2, offenceDetailsList3);
        final List<List<OffenceDetails>> actualOffenceDetailsList = offenceDetailsArgumentCaptor.getAllValues();

        assertEquals(expectedOffenceDetailsList, actualOffenceDetailsList);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(chargeEnvelopeCaptor.capture());

        final Envelope<SubmitChargeProsecutionWithSubmissionId> sentEnvelope = chargeEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitChargeProsecutionWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.charge-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    private SubmitChargeProsecutionHttpWithOucode getChargeProsecutionCase() {
        final List<OffenceDetails> offenceDetailsDuplicateSequenceNumbers = getOffenceDetails();
        final ChargeDefendant chargeDefendant1 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(0))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(1))
                                .build()))
                .build();
        final ChargeDefendant chargeDefendant2 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(2))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(3))
                                .build()))
                .build();
        final ChargeDefendant chargeDefendant3 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(4))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(5))
                                .build()))
                .build();

        return submitChargeProsecutionHttpWithOucode()
                .withDefendants(asList(
                        chargeDefendant1,
                        chargeDefendant2,
                        chargeDefendant3))
                .withProsecutionSubmissionDetails(chargeProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE_UPPER_CASE)
                        .build())
                .withOucode(OUCODE)
                .build();
    }

    private SubmitChargeProsecutionHttpWithOucode getChargeProsecutionCaseWithInvalidUrn() {
        final List<OffenceDetails> offenceDetailsDuplicateSequenceNumbers = getOffenceDetails();
        final ChargeDefendant chargeDefendant1 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(0))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(1))
                                .build()))
                .build();
        final ChargeDefendant chargeDefendant2 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(2))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(3))
                                .build()))
                .build();
        final ChargeDefendant chargeDefendant3 = chargeDefendant()
                .withOffences(asList(
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(4))
                                .build(),
                        chargeOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(5))
                                .build()))
                .build();

        return submitChargeProsecutionHttpWithOucode()
                .withDefendants(asList(
                        chargeDefendant1,
                        chargeDefendant2,
                        chargeDefendant3))
                .withProsecutionSubmissionDetails(chargeProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE_UPPER_CASE)
                        .withUrn(INVALID_URN)
                        .build())
                .withOucode(OUCODE)
                .build();
    }

    @Test
    public void handleRequisitionProsecution() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitRequisitionProsecutionHttpWithOucode payload = getRequisitionProsecutionCase();
        final SubmitRequisitionProsecutionWithSubmissionId convertedPayload =
                submitRequisitionProsecutionWithSubmissionId()
                        .withDefendants(payload.getDefendants())
                        .withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                        .withSubmissionId(SUBMISSION_ID)
                        .build();

        final Envelope<SubmitRequisitionProsecutionHttpWithOucode> originalEnvelope = createSubmitRequisitionProsecutionEnvelope(payload);
        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandAPIV2.submitRequisitionProsecution(originalEnvelope);

        //then
        verify(offenceValidator, times(3)).validate(offenceDetailsArgumentCaptor.capture(), any(Map.class));

        final List<OffenceDetails> offenceDetailsList1 = asList(payload.getDefendants().get(0).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(0).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList2 = asList(payload.getDefendants().get(1).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(1).getOffences().get(1).getOffenceDetails());

        final List<OffenceDetails> offenceDetailsList3 = asList(payload.getDefendants().get(2).getOffences().get(0).getOffenceDetails(),
                payload.getDefendants().get(2).getOffences().get(1).getOffenceDetails());

        final List<List<OffenceDetails>> expectedOffenceDetailsList = asList(offenceDetailsList1, offenceDetailsList2, offenceDetailsList3);
        final List<List<OffenceDetails>> actualOffenceDetailsList = offenceDetailsArgumentCaptor.getAllValues();

        assertEquals(expectedOffenceDetailsList, actualOffenceDetailsList);

        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(requisitionEnvelopeCaptor.capture());

        final Envelope<SubmitRequisitionProsecutionWithSubmissionId> sentEnvelope = requisitionEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitRequisitionProsecutionWithSubmissionId sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.requisition-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    private SubmitRequisitionProsecutionHttpWithOucode getRequisitionProsecutionCase() {
        final List<OffenceDetails> offenceDetailsDuplicateSequenceNumbers = getOffenceDetails();
        final RequisitionDefendant requisitionDefendant1 = requisitionDefendant()
                .withOffences(asList(
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(0))
                                .build(),
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(1))
                                .build()))
                .build();
        final RequisitionDefendant requisitionDefendant2 = requisitionDefendant()
                .withOffences(asList(
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(2))
                                .build(),
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(3))
                                .build()))
                .build();
        final RequisitionDefendant requisitionDefendant3 = requisitionDefendant()
                .withOffences(asList(
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(4))
                                .build(),
                        requisitionOffence()
                                .withOffenceDetails(offenceDetailsDuplicateSequenceNumbers.get(5))
                                .build()))
                .build();

        return submitRequisitionProsecutionHttpWithOucode()
                .withDefendants(asList(
                        requisitionDefendant1,
                        requisitionDefendant2,
                        requisitionDefendant3))
                .withProsecutionSubmissionDetails(requisitionProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE_UPPER_CASE)
                        .build())
                .withOucode(OUCODE)
                .build();
    }

    private List<OffenceDetails> getOffenceDetails() {
        final OffenceDetails offenceDetails1 = offenceDetails().withCjsOffenceCode("CA03013").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails2 = offenceDetails().withCjsOffenceCode("CA03014").withOffenceSequenceNo(1).build();
        final OffenceDetails offenceDetails3 = offenceDetails().withCjsOffenceCode("CA03015").withOffenceSequenceNo(2).build();
        final OffenceDetails offenceDetails4 = offenceDetails().withCjsOffenceCode("CA03016").withOffenceSequenceNo(2).build();
        final OffenceDetails offenceDetails5 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(3).build();
        final OffenceDetails offenceDetails6 = offenceDetails().withCjsOffenceCode("CA03017").withOffenceSequenceNo(4).build();
        return asList(offenceDetails1, offenceDetails2, offenceDetails3, offenceDetails4, offenceDetails5, offenceDetails6);
    }

    @Test
    public void handleSjpProsecution() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        final SubmitSjpProsecutionHttpV2 payload = SubmitSjpProsecutionHttpV2
                .submitSjpProsecutionHttpV2()
                .withDefendant(SjpDefendant.sjpDefendant().build())
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE_UPPER_CASE)
                        .build())
                .build();
        final SubmitSjpProsecution convertedPayload = SubmitSjpProsecution
                .submitSjpProsecution()
                .withDefendant(SjpDefendant.sjpDefendant().build())
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails().build())
                .withSubmissionId(SUBMISSION_ID)
                .build();

        final SubmitSjpProsecutionHttpWithOucode submitSjpProsecutionHttpWithOucode =
                submitSjpProsecutionHttpWithOucode().withProsecutionSubmissionDetails(payload.getProsecutionSubmissionDetails())
                        .withDefendant(payload.getDefendant())
                        .withOucode(OUCODE)
                        .build();


        final Envelope<SubmitSjpProsecutionHttpWithOucode> originalEnvelope = createSubmitSjpProsecutionEnvelope(submitSjpProsecutionHttpWithOucode);
        final Map<String, List<String>> noViolations = new HashMap<>();
        final JsonEnvelope submissionEnvelope =  createEnvelope("hmcts.cjs.submission.v2",
                createObjectBuilder()
                        .add("submissionId",SUBMISSION_ID.toString())
                        .add("oucode",  originalEnvelope.payload().getOucode())
                        .build()
        );

        final JsonObject submissionResponseObject = createObjectBuilder().add("status", SubmissionStatus.REJECTED.toString()).build();

        final Pair<SubmitSjpProsecutionHttpV2, UUID> source = new ImmutablePair<>(payload, SUBMISSION_ID);
        when(submissionQueryView.querySubmissionV2(any())).thenReturn(submissionResponseObject);
        when(systemIdMapperService.getSubmissionIdForUrnWithMatchFound(payload.getProsecutionSubmissionDetails().getUrn())).thenReturn(new ImmutablePair<>(SUBMISSION_ID, Boolean.FALSE));
        when(submitSjpProsecutionV2Converter.convert(source)).thenReturn(convertedPayload);
        when(submitSjpProsecutionHttpV2Validator.validate(payload)).thenReturn(noViolations);

        final Envelope<UrlResponse> stagingProsecutorsResponseEnvelope = stagingProsecutorsCommandAPIV2.submitSJPProsecution(originalEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(stagingProsecutorsResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(stagingProsecutorsResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(sjpEnvelopeCaptor.capture());

        final Envelope<SubmitSjpProsecution> sentEnvelope = sjpEnvelopeCaptor.getValue();
        final Metadata sentEnvelopeMetadata = sentEnvelope.metadata();
        final SubmitSjpProsecution sentEnvelopePayload = sentEnvelope.payload();

        assertThat(sentEnvelopeMetadata.name(), equalTo("stagingprosecutors.command.sjp-prosecution"));
        assertThat(sentEnvelopePayload, equalTo(convertedPayload));
    }

    @Test
    public void shouldHandleSubmitMaterial() {

        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE_UPPER_CASE)
                .add("materialType", "SJPN")
                .add("oucode", OUCODE)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV2.submitMaterial(requestEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material"));

        final JsonObject payloadWithSubmissionId = createObjectBuilder()
                .add("submissionId", SUBMISSION_ID.toString())
                .add("materialId", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE_UPPER_CASE)
                .add("materialType", "SJPN")
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldHandleSubmitMaterialWithPtiUrn() {

        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final String ptiUrn = "ptiUrn01";

        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("ptiUrn", ptiUrn)
                .add("materialType", "SJPN")
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material-with-ptiurn", payload);

        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV2.submitMaterialWithPtiUrn(requestEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material"));

        final JsonObject payloadWithSubmissionId = createObjectBuilder()
                .add("submissionId", SUBMISSION_ID.toString())
                .add("materialId", MATERIAL_ID.toString())
                .add("caseUrn", ptiUrn)
                .add("materialType", "SJPN")
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldHandleSubmitMaterialWithOptionalMetadataFields() {

        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        when(uuidProducer.generateUUID()).thenReturn(SUBMISSION_ID);

        final JsonObject payload = createObjectBuilder()
                .add("material", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE)
                .add("materialType", "SJPN")
                .add("defendantId", "DefendantA001")
                .add("oucode", OUCODE)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", payload);
        final Envelope<UrlResponse> submitMaterialResponseEnvelope = stagingProsecutorsCommandAPIV2.submitMaterial(requestEnvelope);

        //then
        final String expectedStatusURL = "test-base-url/" + SUBMISSION_ID;
        assertThat(submitMaterialResponseEnvelope.payload().getStatusURL(), equalTo(expectedStatusURL));
        assertThat(submitMaterialResponseEnvelope.payload().getSubmissionId(), equalTo(SUBMISSION_ID));

        verify(sender).send(materialEnvelopeCaptor.capture());

        final Envelope sentEnvelope = materialEnvelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(requestEnvelope)
                .withName("stagingprosecutors.command.submit-material"));

        final JsonObject payloadWithSubmissionId = Json.createObjectBuilder()
                .add("submissionId", SUBMISSION_ID.toString())
                .add("materialId", MATERIAL_ID.toString())
                .add("caseUrn", "caseUrn01")
                .add("prosecutingAuthority", OUCODE)
                .add("materialType", "SJPN")
                .add("defendantId", "DefendantA001")
                .build();

        assertThat(sentEnvelope.payload(), equalTo(payloadWithSubmissionId));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadFailsSchemaValidation() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        doThrow(new JsonSchemaValidationException("Schema violations"))
                .when(jsonSchemaValidator).validate(any(), eq("stagingprosecutors.submit-material"));

        final JsonObject invalidPayload = createObjectBuilder()
                .add("materialId", "not_A_valid_UUID")
                .add("oucode", OUCODE)
                .add("prosecutingAuthority", OUCODE)
                .build();

        final JsonEnvelope requestEnvelope = createEnvelope("stagingprosecutors.submit-material", invalidPayload);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitMaterial(requestEnvelope));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfRequestPayloadFailsPayloadValidation() {
        stagingProsecutorsCommandAPIV2.baseResponseURL = "test-base-url/";
        // the content of the violation does not matter, any number of violations will throw an error
        final HashMap<String, List<String>> violations = new HashMap<>();
        violations.put("CODE_1", Collections.emptyList());
        when(submitSjpProsecutionHttpV2Validator.validate(any())).thenReturn(violations);

        final SubmitSjpProsecutionHttpWithOucode payload = submitSjpProsecutionHttpWithOucode()
                .withOucode(OUCODE)
                .withProsecutionSubmissionDetails(sjpProsecutionSubmissionDetails()
                        .withProsecutingAuthority(OUCODE)
                        .build())
                .build();

        final Envelope<SubmitSjpProsecutionHttpWithOucode> envelope = createSubmitSjpProsecutionEnvelope(payload);

        assertThrows(BadRequestException.class, () -> stagingProsecutorsCommandAPIV2.submitSJPProsecution(envelope));
    }

    private Envelope<SubmitRequisitionProsecutionHttpWithOucode> createSubmitRequisitionProsecutionEnvelope(final SubmitRequisitionProsecutionHttpWithOucode payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-requisition-prosecution")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private Envelope<SubmitChargeProsecutionHttpWithOucode> createSubmitChargeProsecutionEnvelope(final SubmitChargeProsecutionHttpWithOucode payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-charge-prosecution")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private Envelope<SubmitSummonsProsecutionHttpWithOucode> createSubmitSummonsProsecutionEnvelope(final SubmitSummonsProsecutionHttpWithOucode payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-summons-prosecution")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }

    private Envelope<SubmitSjpProsecutionHttpWithOucode> createSubmitSjpProsecutionEnvelope(final SubmitSjpProsecutionHttpWithOucode payload) {
        final Metadata metadata = Envelope.metadataBuilder().withId(randomUUID())
                .withName("stagingprosecutors.submit-sjp-prosecution")
                .createdAt(now()).build();

        return Envelope.envelopeFrom(metadata, payload);
    }
}
