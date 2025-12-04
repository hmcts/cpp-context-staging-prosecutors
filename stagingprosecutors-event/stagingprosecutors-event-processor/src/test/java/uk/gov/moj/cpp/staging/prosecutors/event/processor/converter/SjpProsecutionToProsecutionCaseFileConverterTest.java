package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.SjpDefendantToProsecutionCaseFileDefendantConverterTest.assertProsecutionCaseFileDefendantMatchesProsecutionDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverterTest.assertProsecutionCaseFileProsecutorMatchesProsecutionSubmissionDetails;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpProsecutionReceived;
import static uk.gov.moj.cpp.staging.prosecutors.test.util.Prosecutors.prosecutorsSjpProsecutionSubmissionDetails;

import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SjpProsecutionToProsecutionCaseFileConverterTest {

    private final StoppedClock clock = new StoppedClock(now());

    @Test
    public void shouldConvertProsecutionToProsecutionCaseFile() {
        final UUID caseFileId = randomUUID();
        final ZonedDateTime dateReceived = clock.now();
        final SjpProsecutionToProsecutionCaseFileConverter converter = new SjpProsecutionToProsecutionCaseFileConverter(caseFileId, dateReceived);
        final SjpProsecutionReceived sjpProsecutionReceived = prosecutorsSjpProsecutionReceived();
        final InitiateProsecution prosecutorsCaseFileInitiateSjpProsecution = converter.convert(sjpProsecutionReceived);
        final CaseDetails caseDetails = prosecutorsCaseFileInitiateSjpProsecution.getCaseDetails();

        assertThat(caseDetails, notNullValue());
        assertThat(prosecutorsCaseFileInitiateSjpProsecution.getExternalId(), is(sjpProsecutionReceived.getSubmissionId()));
        assertThat(prosecutorsCaseFileInitiateSjpProsecution.getCaseDetails().getProsecutorCaseReference(), is(sjpProsecutionReceived.getProsecutionSubmissionDetails().getUrn()));
        final Prosecutor prosecutionCaseFileProsecutor = caseDetails.getProsecutor();
        final Defendant prosecutionCaseFileDefendant = prosecutorsCaseFileInitiateSjpProsecution.getDefendants().get(0);
        assertProsecutionCaseFileProsecutorMatchesProsecutionSubmissionDetails(prosecutionCaseFileProsecutor, prosecutorsSjpProsecutionSubmissionDetails());
        assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(prosecutionCaseFileDefendant, prosecutorsSjpDefendant());
        assertThat(caseDetails.getCaseId(), is(caseFileId));
        assertThat(caseDetails.getDateReceived(), is(dateReceived.toLocalDate()));
    }


}