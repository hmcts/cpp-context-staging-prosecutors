package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsProsecutionReceived;

import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ProsecutionCaseToProsecutionCaseFileConverterTest {

    private final StoppedClock clock = new StoppedClock(now());

    @Test
    public void shouldConvertProsecutionToProsecutionCaseFile() {
        final UUID caseFileId = UUID.randomUUID();
        final ZonedDateTime dateReceived = clock.now();
        final ProsecutionCaseToProsecutionCaseFileConverter converter = new ProsecutionCaseToProsecutionCaseFileConverter(caseFileId, dateReceived);
        final ProsecutionReceived prosecutionReceived = prosecutorsProsecutionReceived();
        final InitiateProsecution prosecutorsCaseFileInitiateCCProsecution = converter.convert(prosecutionReceived);

        assertThat(prosecutorsCaseFileInitiateCCProsecution.getChannel(), is(Channel.CPPI));
        assertThat(prosecutorsCaseFileInitiateCCProsecution.getExternalId(), is(prosecutionReceived.getSubmissionId()));
        assertCaseDetails(prosecutorsCaseFileInitiateCCProsecution.getCaseDetails(), prosecutionReceived.getProsecutionSubmissionDetails(), caseFileId);
    }

    private void assertCaseDetails(final CaseDetails pcfCaseDetails, final ProsecutionSubmissionDetails stagingProsecutionSubmissionDetails, final UUID caseId) {
        assertThat(pcfCaseDetails, notNullValue());

        assertThat(pcfCaseDetails.getCaseId(), is(caseId));
        assertThat(pcfCaseDetails.getDateReceived(), is(clock.now().toLocalDate()));
        assertThat(pcfCaseDetails.getInitiationCode(), is(stagingProsecutionSubmissionDetails.getInitiationCode().toString()));
        assertThat(pcfCaseDetails.getOriginatingOrganisation(), is(stagingProsecutionSubmissionDetails.getProsecutingAuthority()));
        assertThat(pcfCaseDetails.getProsecutor().getInformant(), is(stagingProsecutionSubmissionDetails.getInformant()));
        assertThat(pcfCaseDetails.getProsecutor().getProsecutingAuthority(), is(stagingProsecutionSubmissionDetails.getProsecutingAuthority()));
        assertThat(pcfCaseDetails.getSummonsCode(), is(stagingProsecutionSubmissionDetails.getSummonsCode()));
        assertThat(pcfCaseDetails.getCaseMarkers(), is(singletonList(caseMarker()
                .withMarkerTypeCode(stagingProsecutionSubmissionDetails.getCaseMarker())
                .build())));
        assertThat(pcfCaseDetails.getProsecutorCaseReference(), is(stagingProsecutionSubmissionDetails.getUrn()));

        assertNull(pcfCaseDetails.getOtherPartyOfficerInCase());
        assertNull(pcfCaseDetails.getCpsOrganisation());
    }

}