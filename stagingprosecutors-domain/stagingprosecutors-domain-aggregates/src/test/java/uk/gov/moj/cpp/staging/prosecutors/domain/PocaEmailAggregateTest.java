package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.BoxHearingRequest.boxHearingRequest;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication.courtApplication;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentNotValidated;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentValidated;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PocaEmailAggregateTest {

    @InjectMocks
    PocaEmailAggregate pocaEmailAggregate;

    @Test
    public void shouldValidatePocaEmail() {
        final UUID pocaId = randomUUID();
        final String senderEmail = "test@test.com";
        final String emailSubject = "test subject";

        final Stream<Object> eventStream = pocaEmailAggregate.pocaEmailValidated(pocaId, senderEmail, emailSubject, courtApplication().build(), boxHearingRequest().build());
        final PocaDocumentValidated pocaDocumentValidated = (PocaDocumentValidated) eventStream.findFirst().get();
        assertThat(pocaDocumentValidated.getPocaFileId(), is(pocaId));
        assertThat(pocaDocumentValidated.getSenderEmail(), is(senderEmail));
        assertThat(pocaDocumentValidated.getEmailSubject(), is(emailSubject));
        assertThat(pocaDocumentValidated.getCourtApplication(), notNullValue());
        assertThat(pocaDocumentValidated.getBoxHearingRequest(), notNullValue());
    }


    @Test
    public void shouldNotValidatePocaEmail() {
        final UUID pocaId = randomUUID();
        final String senderEmail = "test@test.com";
        final String emailSubject = "test subject";

        final Stream<Object> eventStream = pocaEmailAggregate.pocaEmailNotValidated(pocaId, senderEmail, emailSubject, Collections.emptyList() );
        final PocaDocumentNotValidated pocaDocumentNotValidated = (PocaDocumentNotValidated) eventStream.findFirst().get();
        assertThat(pocaDocumentNotValidated.getPocaFileId(), is(pocaId));
        assertThat(pocaDocumentNotValidated.getSenderEmail(), is(senderEmail));
        assertThat(pocaDocumentNotValidated.getEmailSubject(), is(emailSubject));
    }
}