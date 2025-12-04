package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsBcmReceivedDetails.cpsBcmReceivedDetails;
import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutionCaseSubject.prosecutionCaseSubject;
import static java.util.Optional.ofNullable;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeBcmReceived;

import java.util.List;
import java.util.stream.Collectors;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsBcmReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;

public class CpsServeBcmReceivedToCpsBcmReceivedDetailsConverter implements Converter<CpsServeBcmReceived, CpsBcmReceivedDetails> {

    @Override
    public CpsBcmReceivedDetails convert(final CpsServeBcmReceived cpsServeBcmReceived) {
        final CpsProsecutionCaseSubject cpsProsecutionCaseSubject = cpsServeBcmReceived.getProsecutionCaseSubject();
        final CpsBcmReceivedDetails.Builder builder = cpsBcmReceivedDetails()
                .withCpsDefendantOffences(buildCpsDefendantOffencesListFromCpsReceived(cpsServeBcmReceived));

        builder.withProsecutionCaseSubject(prosecutionCaseSubject()
                .withProsecutingAuthority(cpsProsecutionCaseSubject.getProsecutingAuthority())
                .withUrn(cpsProsecutionCaseSubject.getUrn())
                .build());

        builder.withSubmissionId(cpsServeBcmReceived.getSubmissionId());

        ofNullable(cpsServeBcmReceived.getTag()).ifPresent(builder::withTag);
        ofNullable(cpsServeBcmReceived.getEvidencePrePTPH()).ifPresent(builder::withEvidencePrePTPH);
        ofNullable(cpsServeBcmReceived.getEvidencePostPTPH()).ifPresent(builder::withEvidencePostPTPH);
        ofNullable(cpsServeBcmReceived.getOtherInformation()).ifPresent(builder::withOtherInformation);

        ofNullable(cpsServeBcmReceived
                .getSubmissionStatus())
                .ifPresent(submissionStatus -> SubmissionStatus
                        .valueFor(submissionStatus.name())
                        .ifPresent(builder::withSubmissionStatus));

        return builder.build();
    }

    private List<CpsDefendantOffences> buildCpsDefendantOffencesListFromCpsReceived(final CpsServeBcmReceived cpsServePetReceived) {
        return cpsServePetReceived.getDefendantOffencesSubject().stream().map(defendantOffencesSubject -> new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(defendantOffencesSubject)).collect(Collectors.toList());
    }
}
