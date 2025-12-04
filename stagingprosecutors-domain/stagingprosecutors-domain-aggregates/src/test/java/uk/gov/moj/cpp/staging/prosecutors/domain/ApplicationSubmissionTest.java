package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationSubmissionTest {

    @InjectMocks
    private ApplicationSubmission applicationSubmission;

    @Test
    public void shouldRaiseApplicationSubmittedEvent() {

        final CourtApplication courtApplication = CourtApplication.courtApplication().withId(randomUUID()).build();
        final BoxHearingRequest boxHearingRequest = BoxHearingRequest.boxHearingRequest().withId(randomUUID()).build();
        final UUID pocaFileId = randomUUID();
        final String senderEmail = "sender@hmcts.net";
        final String emailSubject = "emailSubject";

        final Stream<Object> eventStream = applicationSubmission.receiveSubmission(courtApplication, boxHearingRequest, pocaFileId, senderEmail, emailSubject);

        final ApplicationSubmitted applicationSubmitted = (ApplicationSubmitted) eventStream.findFirst().get();
        assertThat(applicationSubmitted.getEmailSubject(), is(emailSubject));
        assertThat(applicationSubmitted.getCourtApplication(), is(courtApplication));
        assertThat(applicationSubmitted.getSenderEmail(), is(senderEmail));
        assertThat(applicationSubmitted.getBoxHearingRequest(), is(boxHearingRequest));
        assertThat(applicationSubmitted.getPocaFileId(), is(pocaFileId));
    }


}
