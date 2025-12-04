package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPetReceivedDetails.cpsPetReceivedDetails;
import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutionCaseSubject.prosecutionCaseSubject;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPetReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;

public class CpsServePetReceivedToCpsPetReceivedDetailsConverter implements Converter<CpsServePetReceived, CpsPetReceivedDetails> {

    @Override
    public CpsPetReceivedDetails convert(final CpsServePetReceived cpsServePetReceived) {
        final CpsPetReceivedDetails.Builder builder = cpsPetReceivedDetails()
                .withCpsDefendantOffences(buildCpsDefendantOffencesListFromCpsReceived(cpsServePetReceived))
                .withPetFormData(new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived))
                .withReviewingLawyer(cpsServePetReceived.getReviewingLawyer())
                .withProsecutionCaseProgressionOfficer(cpsServePetReceived.getProsecutionCaseProgressionOfficer())
                .withProsecutionCaseSubject(prosecutionCaseSubject()
                        .withProsecutingAuthority(cpsServePetReceived.getProsecutionCaseSubject().getProsecutingAuthority())
                        .withUrn(cpsServePetReceived.getProsecutionCaseSubject().getUrn())
                        .build())
                .withSubmissionId(cpsServePetReceived.getSubmissionId())
                .withIsYouth(cpsServePetReceived.getIsYouth());
        final Optional<SubmissionStatus> submissionStatusOptional = SubmissionStatus.valueFor(cpsServePetReceived.getSubmissionStatus().name());
        if (submissionStatusOptional.isPresent()) {
            builder.withSubmissionStatus(submissionStatusOptional.get());
        }

        return builder.build();
    }

    private List<CpsDefendantOffences> buildCpsDefendantOffencesListFromCpsReceived(final CpsServePetReceived cpsServePetReceived) {
        return cpsServePetReceived.getDefendantOffencesSubjects().stream().map(defendantOffencesSubject -> new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(defendantOffencesSubject)).collect(Collectors.toList());
    }
}
