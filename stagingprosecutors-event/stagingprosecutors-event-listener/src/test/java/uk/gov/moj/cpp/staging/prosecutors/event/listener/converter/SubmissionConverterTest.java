package uk.gov.moj.cpp.staging.prosecutors.event.listener.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType.MATERIAL;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.Submission;

import java.time.ZonedDateTime;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionConverterTest {

    @InjectMocks
    private SubmissionConverter submissionConverter;

    @Test
    public void shouldTestSubmissionConverterWithProsecutionSubject() {

        final MaterialSubmittedV3 materialSubmitted = createMaterialSubmittedV3(true);
        final Envelope<MaterialSubmittedV3> envelope = newEnvelope("stagingprosecutors.event.material-submitted-v3", materialSubmitted);

        final Submission submission = submissionConverter.convert(envelope);

        assertThat(submission, notNullValue());
        assertThat(submission.getSubmissionId(), is(materialSubmitted.getSubmissionId()));
        assertThat(submission.getSubmissionStatus(), is(materialSubmitted.getSubmissionStatus().toString()));
        assertThat(submission.getCaseUrn(), is(materialSubmitted.getProsecutionCaseSubject().getCaseUrn()));
        assertThat(submission.getOuCode(), is(materialSubmitted.getProsecutionCaseSubject().getProsecutingAuthority()));
        assertThat(submission.getType(), is(MATERIAL));
        assertThat(submission.getCpsCase(), is(false));
        assertThat(materialSubmitted.getCourtApplicationSubject(), is(nullValue()));
    }

    @Test
    public void shouldTestSubmissionConverterWithCourtApplicationSubject() {

        final MaterialSubmittedV3 materialSubmitted = createMaterialSubmittedV3(false);
        final Envelope<MaterialSubmittedV3> envelope = newEnvelope("stagingprosecutors.event.material-submitted-v3", materialSubmitted);

        final Submission submission = submissionConverter.convert(envelope);

        assertThat(submission, notNullValue());
        assertThat(submission.getSubmissionId(), is(materialSubmitted.getSubmissionId()));
        assertThat(submission.getSubmissionStatus(), is(materialSubmitted.getSubmissionStatus().toString()));
        assertThat(submission.getApplicationId(), is(materialSubmitted.getCourtApplicationSubject().getCourtApplicationId()));
        assertThat(submission.getType(), is(MATERIAL));
        assertThat(submission.getCpsCase(), is(false));
        assertThat(materialSubmitted.getProsecutionCaseSubject(), is(nullValue()));
    }

    private <T> Envelope<T> newEnvelope(final String name, T payload) {
        return envelopeFrom(metadataWithRandomUUID(name).createdAt(ZonedDateTime.now(UTC)), payload);
    }

    private MaterialSubmittedV3 createMaterialSubmittedV3(final boolean flag) {

        final MaterialSubmittedV3.Builder builder = new MaterialSubmittedV3.Builder()
                .withSubmissionId(randomUUID())
                .withSubmissionStatus(SubmissionStatus.PENDING)
                .withIsCpsCase(false);
        if (flag) {
            builder.withProsecutionCaseSubject(createProsecutionCaseSubject());
        } else {
            builder.withCourtApplicationSubject(createCourtApplicationSubject());
        }

        return builder.build();
    }

    private ProsecutionCaseSubject createProsecutionCaseSubject(){
        return new ProsecutionCaseSubject.Builder()
                .withCaseUrn(STRING.next())
                .withProsecutingAuthority(STRING.next())
                .build();
    }

    private CourtApplicationSubject createCourtApplicationSubject(){
        return new CourtApplicationSubject.Builder()
                .withCourtApplicationId(randomUUID())
                .build();
    }
}
