package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.InitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

public class SjpProsecutionToProsecutionCaseFileConverter implements Converter<SjpProsecutionReceived, InitiateProsecution> {

    private final UUID caseFileId;
    private final ZonedDateTime dateReceived;

    public SjpProsecutionToProsecutionCaseFileConverter(final UUID caseFileId, final ZonedDateTime dateReceived) {
        this.caseFileId = caseFileId;
        this.dateReceived = dateReceived;
    }

    @Override
    public InitiateProsecution convert(final SjpProsecutionReceived sjpProsecutionReceived) {

        final Converter<SjpDefendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> prosecutionDefendantToProsecutionCaseFileDefendantConverter
                = new SjpDefendantToProsecutionCaseFileDefendantConverter(sjpProsecutionReceived.getProsecutionSubmissionDetails().getWrittenChargePostingDate());

        final Converter<SjpProsecutionSubmissionDetails, Prosecutor> prosecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter
                = new SjpProsecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter();

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant defendant =
                prosecutionDefendantToProsecutionCaseFileDefendantConverter.convert(sjpProsecutionReceived.getDefendant());

        final Prosecutor prosecutor
                = prosecutionSubmissionDetailsToProsecutionCaseFileProsecutorConverter.convert(sjpProsecutionReceived.getProsecutionSubmissionDetails());

        return InitiateProsecution.initiateProsecution()
                .withDefendants(ImmutableList.of(defendant))
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(caseFileId)
                        .withDateReceived(dateReceived.toLocalDate())
                        .withProsecutorCaseReference(sjpProsecutionReceived.getProsecutionSubmissionDetails().getUrn())
                        .withProsecutor(prosecutor)
                        .withInitiationCode(InitiationCode.J.name())
                        .withOriginatingOrganisation(sjpProsecutionReceived.getProsecutionSubmissionDetails().getProsecutingAuthority())
                        .build())
                .withChannel(CPPI)
                .withExternalId(sjpProsecutionReceived.getSubmissionId())
                .build();
    }
}