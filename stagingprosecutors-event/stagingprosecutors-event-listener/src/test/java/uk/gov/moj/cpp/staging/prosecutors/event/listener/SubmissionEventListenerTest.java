package uk.gov.moj.cpp.staging.prosecutors.event.listener;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3.materialSubmittedV3;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected.materialSubmissionRejected;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful.materialSubmissionSuccessful;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted.materialSubmitted;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived.prosecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived.sjpProsecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings.submissionPendingWithWarnings;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected.submissionRejected;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING_WITH_WARNINGS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS_WITH_WARNINGS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful.submissionSuccessful;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings.submissionSuccessfulWithWarnings;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.MATERIAL;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.PROSECUTION;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.event.listener.converter.SubmissionConverter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionSuccessfulWithWarnings;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SubmissionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionEventListenerTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ObjectToJsonObjectConverter converter;

    @InjectMocks
    private SubmissionEventListener submissionEventListener;

    @Captor
    private ArgumentCaptor<Submission> argumentCaptor;

    @Mock
    SubmissionConverter submissionConverter;

    private final UUID submissionId = randomUUID();

    @Test
    public void shouldHandleSubmissionRelatedEvents() {
        assertThat(new SubmissionEventListener(), isHandler(EVENT_LISTENER)
                .with(allOf(
                        method("sjpSubmissionRequestReceived").thatHandles("stagingprosecutors.event.sjp-prosecution-received"),
                        method("submissionRequestReceived").thatHandles("stagingprosecutors.event.prosecution-received"),
                        method("submissionSuccessfulReceived").thatHandles("stagingprosecutors.event.submission-successful"),
                        method("submissionRejected").thatHandles("stagingprosecutors.event.submission-rejected"),
                        method("materialSubmitted").thatHandles("stagingprosecutors.event.material-submitted"),
                        method("materialSubmissionSuccessfulReceived").thatHandles("stagingprosecutors.event.material-submission-successful"),
                        method("materialSubmissionRejected").thatHandles("stagingprosecutors.event.material-submission-rejected")
                )));
    }

    @Test
    public void shouldHandleSubmissionRequestReceived() {
        String caseUrn = "TVL1234";

        final ProsecutionReceived prosecutionReceived =
                prosecutionReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionSubmissionDetails(
                                ProsecutionSubmissionDetails
                                        .prosecutionSubmissionDetails()
                                        .withProsecutingAuthority("TVL")
                                        .withUrn(caseUrn)
                                        .build()
                        )
                        .build();

        final Envelope<ProsecutionReceived> envelope = newEnvelope("stagingprosecutors.event.prosecution-received", prosecutionReceived);

        submissionEventListener.submissionRequestReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.toString()));
        assertThat(submission.getType(), is(PROSECUTION));
        assertThat(submission.getCaseUrn(), is(caseUrn));
        assertThat(submission.getOuCode(), is(prosecutionReceived.getProsecutionSubmissionDetails().getProsecutingAuthority()));
        assertThat(submission.getReceivedAt(), is(envelope.metadata().createdAt().get()));
        assertThat(submission.getCompletedAt(), is(nullValue()));
        assertThat(submission.getErrors(), is(nullValue()));
        assertThat(submission.getWarnings(), is(nullValue()));
    }

    @Test
    public void shouldHandleSjpSubmissionRequestReceived() {
        String caseUrn = "TVL1234";

        final SjpProsecutionReceived sjpProsecutionReceived =
                sjpProsecutionReceived().withSubmissionId(submissionId)
                        .withSubmissionStatus(PENDING)
                        .withProsecutionSubmissionDetails(
                                SjpProsecutionSubmissionDetails
                                        .sjpProsecutionSubmissionDetails()
                                        .withProsecutingAuthority("TVL")
                                        .withUrn(caseUrn)
                                        .build()
                        )
                        .build();

        final Envelope<SjpProsecutionReceived> envelope = newEnvelope("stagingprosecutors.event.sjp-prosecution-received", sjpProsecutionReceived);

        submissionEventListener.sjpSubmissionRequestReceived(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submission = argumentCaptor.getValue();

        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(PENDING.toString()));
        assertThat(submission.getType(), is(PROSECUTION));
        assertThat(submission.getCaseUrn(), is(caseUrn));
        assertThat(submission.getOuCode(), is(sjpProsecutionReceived.getProsecutionSubmissionDetails().getProsecutingAuthority()));
        assertThat(submission.getReceivedAt(), is(envelope.metadata().createdAt().get()));
        assertThat(submission.getCompletedAt(), is(nullValue()));
        assertThat(submission.getErrors(), is(nullValue()));
        assertThat(submission.getWarnings(), is(nullValue()));
    }

    @Test
    public void shouldHandleSubmissionSuccessful() {
        final SubmissionSuccessful submissionSuccessful = submissionSuccessful().withSubmissionId(submissionId).build();

        final Submission savedSubmission = mock(Submission.class);
        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);

        final Envelope<SubmissionSuccessful> envelope = newEnvelope("stagingprosecutors.event.submission-successful", submissionSuccessful);

        submissionEventListener.submissionSuccessfulReceived(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(SUCCESS.toString());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
        verify(savedSubmission, never()).setErrors(any());
    }

    @Test
    public void shouldHandleSubmissionSuccessfulWithWarnings() {
        final Problem warning = newWarning("DEFENDANT_BELOW_18", "dob", "2001-01-01");
        final JsonObject warningAsJson = problemAsJson(warning.getValues().get(0));

        final DefendantProblem defendantWarning = newDefendantWarning("OFFENCE_OUT_OF_TIME", "daysOverdue", "82");
        final JsonObject defendantWarningAsJson = defendantWarningAsJson(defendantWarning);

        final SubmissionSuccessfulWithWarnings submissionSuccessfulWithWarnings
                = submissionSuccessfulWithWarnings().withSubmissionId(submissionId)
                .withWarnings(asList(warning))
                .withDefendantWarnings(asList(defendantWarning))
                .build();

        final Submission savedSubmission = mock(Submission.class);
        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);
        when(converter.convert(warning)).thenReturn(warningAsJson);
        when(converter.convert(defendantWarning)).thenReturn(defendantWarningAsJson);

        final Envelope<SubmissionSuccessfulWithWarnings> envelope = newEnvelope("stagingprosecutors.event.submission-successful-with-warnings", submissionSuccessfulWithWarnings);

        submissionEventListener.submissionSuccessfulReceivedWithWarnings(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(SUCCESS_WITH_WARNINGS.toString());
        verify(savedSubmission).setWarnings(createArrayBuilder().add(warningAsJson).build());
        verify(savedSubmission).setDefendantWarnings(createArrayBuilder().add(defendantWarningAsJson).build());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
        verify(savedSubmission, never()).setErrors(any());
    }


    @Test
    public void shouldHandleSubmissionRejected() {
        final Problem error = newError("DEFENDANT_DOB_IN_FUTURE", "dob", "2050-01-01");

        final SubmissionRejected submissionRejected = submissionRejected()
                .withSubmissionId(submissionId)
                .withErrors(asList(error))
                .build();

        final Submission savedSubmission = mock(Submission.class);
        final Envelope<SubmissionRejected> envelope = newEnvelope("stagingprosecutors.event.submission-rejected", submissionRejected);
        final JsonObject errorAsJson = errorAsJson(error.getValues().get(0));

        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);
        when(converter.convert(error)).thenReturn(errorAsJson);

        submissionEventListener.submissionRejected(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(REJECTED.toString());
        verify(savedSubmission).setErrors(createArrayBuilder().add(errorAsJson).build());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
    }

    @Test
    public void shouldHandleMaterialSubmitted() {
        final String caseUrn = STRING.next();
        final String defendantId = randomUUID().toString();
        final String materialType = STRING.next();
        final String prosecutingAuthority = STRING.next();
        final SubmissionStatus submissionStatus = randomEnum(SubmissionStatus.class).next();
        final UUID materialId = randomUUID();

        final MaterialSubmitted materialSubmitted = materialSubmitted()
                .withCaseUrn(caseUrn)
                .withDefendantId(defendantId)
                .withMaterialType(materialType)
                .withProsecutingAuthority(prosecutingAuthority)
                .withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .withMaterialId(materialId)
                .build();

        final Envelope<MaterialSubmitted> envelope = newEnvelope("stagingprosecutors.event.material-submitted", materialSubmitted);

        submissionEventListener.materialSubmitted(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());
        final Submission submission = argumentCaptor.getValue();
        assertThat(submission.getOuCode(), is(prosecutingAuthority));
        assertThat(submission.getCaseUrn(), is(caseUrn));
        assertThat(submission.getSubmissionId(), is(submissionId));
        assertThat(submission.getSubmissionStatus(), is(submissionStatus.toString()));
        assertThat(submission.getType(), is(MATERIAL));
        assertThat(submission.getErrors(), is(nullValue()));
        assertThat(submission.getWarnings(), is(nullValue()));
        assertThat(submission.getReceivedAt(), is(envelope.metadata().createdAt().get()));
    }

    @Test
    public void shouldHandleMaterialSubmittedV3() {

        final MaterialSubmittedV3 materialSubmitted = materialSubmittedV3().build();
        final Envelope<MaterialSubmittedV3> envelope = newEnvelope("stagingprosecutors.event.material-submitted-v3", materialSubmitted);

        final Submission submission = createSubmissionEntity(envelope);

        when(submissionConverter.convert(any())).thenReturn(submission);

        submissionEventListener.materialSubmittedV3(envelope);

        verify(submissionRepository).save(argumentCaptor.capture());

        final Submission submissionEntity = argumentCaptor.getValue();
        assertThat(submissionEntity.getOuCode(), is(submission.getOuCode()));
        assertThat(submissionEntity.getCaseUrn(), is(submission.getCaseUrn()));
        assertThat(submissionEntity.getSubmissionId(), is(submissionId));
        assertThat(submissionEntity.getSubmissionStatus(), is(submission.getSubmissionStatus()));
        assertThat(submission.getType(), is(MATERIAL));
        assertThat(submission.getErrors(), is(nullValue()));
        assertThat(submission.getWarnings(), is(nullValue()));
        assertThat(submission.getReceivedAt(), is(envelope.metadata().createdAt().get()));
    }


    @Test
    public void shouldHandleMaterialSubmissionSuccessful() {
        final MaterialSubmissionSuccessful materialSubmissionSuccessful = materialSubmissionSuccessful()
                .withSubmissionId(submissionId)
                .build();

        final Submission savedSubmission = mock(Submission.class);
        final Envelope<MaterialSubmissionSuccessful> envelope = newEnvelope("stagingprosecutors.event.material-submission-successful", materialSubmissionSuccessful);

        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);

        submissionEventListener.materialSubmissionSuccessfulReceived(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(SUCCESS.toString());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
    }

    @Test
    public void shouldHandleMaterialSubmissionRejected() {
        final Problem error = newError("INVALID_DOCUMENT_TYPE", "documentType", "PLEA");
        final JsonObject errorAsJson = problemAsJson(error.getValues().get(0));

        final Submission savedSubmission = mock(Submission.class);

        final MaterialSubmissionRejected materialSubmissionSuccessful = materialSubmissionRejected()
                .withSubmissionId(submissionId)
                .withErrors(asList(error))
                .build();

        final Envelope<MaterialSubmissionRejected> envelope = newEnvelope("stagingprosecutors.event.material-submission-rejected", materialSubmissionSuccessful);

        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);
        when(converter.convert(error)).thenReturn(errorAsJson);

        submissionEventListener.materialSubmissionRejected(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(REJECTED.toString());
        verify(savedSubmission).setErrors(createArrayBuilder().add(errorAsJson).build());
        verify(savedSubmission).setCompletedAt(envelope.metadata().createdAt().get());
    }

    @Test
    public void shouldHandleSubmissionPendingWithWarning() {
        final Problem warnings = newError("DEFENDANT_ON_CP", "defendant", "NOT IN CP");

        final SubmissionPendingWithWarnings submissionPendingWithWarnings = submissionPendingWithWarnings()
                .withSubmissionId(submissionId)
                .withWarnings(asList(warnings))
                .build();

        final Submission savedSubmission = mock(Submission.class);
        final Envelope<SubmissionPendingWithWarnings> envelope = newEnvelope("stagingprosecutors.event.submission-pending-with-warnings", submissionPendingWithWarnings);
        final JsonObject errorAsJson = errorAsJson(warnings.getValues().get(0));

        when(submissionRepository.findBy(submissionId)).thenReturn(savedSubmission);
        when(converter.convert(warnings)).thenReturn(errorAsJson);

        submissionEventListener.materialSubmissionPendingWithWarnings(envelope);

        verify(submissionRepository).findBy(submissionId);
        verify(savedSubmission).setSubmissionStatus(PENDING_WITH_WARNINGS.toString());
        verify(savedSubmission).setWarnings(createArrayBuilder().add(errorAsJson).build());
    }


    private static JsonObject problemAsJson(final ProblemValue problemValue) {
        return Json.createObjectBuilder()
                .add("key", problemValue.getKey())
                .add("value", problemValue.getValue())
                .build();
    }

    private static JsonObject errorAsJson(final ProblemValue problemValue) {
        return Json.createObjectBuilder()
                .add("key", problemValue.getKey())
                .add("value", problemValue.getValue())
                .build();
    }

    private static JsonObject defendantWarningAsJson(final DefendantProblem defendantProblem) {
        final Problem problem = defendantProblem.getProblems().get(0);
        final ProblemValue problemValue = problem.getValues().get(0);
        return Json.createObjectBuilder()
                .add("prosecutorDefendantReference", defendantProblem.getProsecutorDefendantReference())
                .add("problems",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("code", problem.getCode())
                                        .add("values", Json.createArrayBuilder()
                                                .add(problemAsJson(problemValue))
                                                .build())
                                        .build())
                                .build())
                .build();
    }

    private static Problem newWarning(final String code, final String key, final String value) {
        return Problem.problem()
                .withCode(code)
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey(key)
                                .withValue(value)
                                .build()))
                .build();
    }

    private static Problem newError(final String code, final String key, final String value) {
        return Problem.problem()
                .withCode(code)
                .withValues(ImmutableList.of(
                        ProblemValue
                                .problemValue()
                                .withKey(key)
                                .withValue(value)
                                .build()))
                .build();
    }

    private static DefendantProblem newDefendantWarning(final String code, final String key, final String value) {
        return DefendantProblem.defendantProblem()
                .withProsecutorDefendantReference("DEFENDANT_REFERENCE")
                .withProblems(asList(newError(code, key, value)))
                .build();
    }

    private <T> Envelope<T> newEnvelope(final String name, T payload) {
        return envelopeFrom(metadataWithRandomUUID(name).createdAt(ZonedDateTime.now(UTC)), payload);
    }

    private Submission createSubmissionEntity(final Envelope<MaterialSubmittedV3> envelope) {
        final Submission submission = new Submission();
        submission.setSubmissionId(submissionId);
        submission.setCaseUrn(STRING.next());
        submission.setOuCode(STRING.next());
        submission.setType(MATERIAL);
        submission.setSubmissionStatus(PENDING.toString());
        submission.setReceivedAt(envelope.metadata().createdAt().get());
        return submission;
    }
}
