package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class ProsecutionCaseToProsecutionCaseFileConverter implements Converter<ProsecutionReceived, InitiateProsecution> {

    private final UUID caseFileId;
    private final ZonedDateTime dateReceived;

    public ProsecutionCaseToProsecutionCaseFileConverter(final UUID caseFileId, final ZonedDateTime dateReceived) {
        this.caseFileId = caseFileId;
        this.dateReceived = dateReceived;
    }

    @Override
    public InitiateProsecution convert(final ProsecutionReceived prosecutionReceived) {

        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantToProsecutionCaseFileDefendantConverter
                = new DefendantToProsecutionCaseFileDefendantConverter(prosecutionReceived.getProsecutionSubmissionDetails());

        return new InitiateProsecution.Builder()
                .withCaseDetails(buildCaseDetails(prosecutionReceived.getProsecutionSubmissionDetails()))
                .withDefendants(buildDefendants(prosecutionReceived, defendantToProsecutionCaseFileDefendantConverter))
                .withChannel(CPPI)
                .withExternalId(prosecutionReceived.getSubmissionId())
                .build();
    }

    private List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> buildDefendants(final ProsecutionReceived prosecutionReceived,
                                                                                             final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantToProsecutionCaseFileDefendantConverter) {
        return prosecutionReceived.getDefendants().stream().map(defendantToProsecutionCaseFileDefendantConverter::convert).collect(toList());
    }

    private CaseDetails buildCaseDetails(final ProsecutionSubmissionDetails prosecutionSubmissionDetails) {
        return CaseDetails.caseDetails()
                .withDateReceived(dateReceived.toLocalDate())
                .withProsecutorCaseReference(prosecutionSubmissionDetails.getUrn())
                .withOtherPartyOfficerInCase(null)
                .withCaseId(caseFileId)
                .withSummonsCode(prosecutionSubmissionDetails.getSummonsCode())
                .withCpsOrganisation(null)
                .withInitiationCode(prosecutionSubmissionDetails.getInitiationCode() != null ? prosecutionSubmissionDetails.getInitiationCode().toString() : null)
                .withOriginatingOrganisation(prosecutionSubmissionDetails.getProsecutingAuthority())
                .withCaseMarkers(buildCaseMarkers(prosecutionSubmissionDetails.getCaseMarker()))
                .withProsecutor(Prosecutor.prosecutor()
                        .withInformant(prosecutionSubmissionDetails.getInformant())
                        .withProsecutingAuthority(prosecutionSubmissionDetails.getProsecutingAuthority()).build())
                .build();
    }

    private List<CaseMarker> buildCaseMarkers(final String caseMarkers) {
        if (caseMarkers == null) {
            return emptyList();
        }
        final String[] caseMarkersArray = caseMarkers.split("\\s+");
        return stream(caseMarkersArray)
                .map(caseMarker -> caseMarker().withMarkerTypeCode(caseMarker).build())
                .distinct()
                .collect(toList());
    }
}