package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServeCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsCotrReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.DefendantSubject;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class CpsServeCotrReceivedToCpsCotrReceivedDetailsConverter implements Converter<CpsServeCotrReceived, CpsCotrReceivedDetails> {

    @Override
    public CpsCotrReceivedDetails convert(final CpsServeCotrReceived cpsServeCotrReceived) {

        final CpsCotrReceivedDetails.Builder cpsCotrReceivedDetails = CpsCotrReceivedDetails.cpsCotrReceivedDetails();
        cpsCotrReceivedDetails.withSubmissionId(cpsServeCotrReceived.getSubmissionId())
                .withSubmissionStatus(SubmissionStatus.valueOf(cpsServeCotrReceived.getSubmissionStatus().name()))
                .withProsecutionCaseSubject(CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                        .withProsecutingAuthority(cpsServeCotrReceived.getProsecutionCaseSubject().getProsecutingAuthority())
                        .withUrn(cpsServeCotrReceived.getProsecutionCaseSubject().getUrn())
                        .build())
                .withDefendantSubject(convertDefendantSubjects(cpsServeCotrReceived))
                .withApplyForThePtrToBeVacated(cpsServeCotrReceived.getApplyForThePtrToBeVacated())
                .withApplyForThePtrToBeVacatedDetails(cpsServeCotrReceived.getApplyForThePtrToBeVacatedDetails())
                .withCertificationDate(cpsServeCotrReceived.getCertificationDate())
                .withCertifyThatTheProsecutionIsTrialReady(cpsServeCotrReceived.getCertifyThatTheProsecutionIsTrialReady())
                .withCertifyThatTheProsecutionIsTrialReadyDetails(cpsServeCotrReceived.getCertifyThatTheProsecutionIsTrialReadyDetails())
                .withFormCompletedOnBehalfOfTheProsecutionBy(cpsServeCotrReceived.getFormCompletedOnBehalfOfTheProsecutionBy())
                .withFurtherInformationToAssistTheCourt(cpsServeCotrReceived.getFurtherInformationToAssistTheCourt())
                .withHasAllDisclosureBeenProvided(cpsServeCotrReceived.getHasAllDisclosureBeenProvided())
                .withHasAllDisclosureBeenProvidedDetails(cpsServeCotrReceived.getHasAllDisclosureBeenProvidedDetails())
                .withHasAllEvidenceToBeReliedOnBeenServed(cpsServeCotrReceived.getHasAllEvidenceToBeReliedOnBeenServed())
                .withHasAllEvidenceToBeReliedOnBeenServedDetails(cpsServeCotrReceived.getHasAllEvidenceToBeReliedOnBeenServedDetails())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(cpsServeCotrReceived.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed())
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails(cpsServeCotrReceived.getHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(cpsServeCotrReceived.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement())
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails(cpsServeCotrReceived.getHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(cpsServeCotrReceived.getHaveEditedAbeInterviewsBeenPreparedAndAgreed())
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails(cpsServeCotrReceived.getHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails())
                .withHaveInterpretersForWitnessesBeenArranged(cpsServeCotrReceived.getHaveInterpretersForWitnessesBeenArranged())
                .withHaveInterpretersForWitnessesBeenArrangedDetails(cpsServeCotrReceived.getHaveInterpretersForWitnessesBeenArrangedDetails())
                .withHaveOtherDirectionsBeenCompliedWith(cpsServeCotrReceived.getHaveOtherDirectionsBeenCompliedWith())
                .withHaveOtherDirectionsBeenCompliedWithDetails(cpsServeCotrReceived.getHaveOtherDirectionsBeenCompliedWithDetails())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(cpsServeCotrReceived.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved())
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails(cpsServeCotrReceived.getHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(cpsServeCotrReceived.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend())
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(cpsServeCotrReceived.getHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(cpsServeCotrReceived.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJury())
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails(cpsServeCotrReceived.getIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails())
                .withIsTheTimeEstimateCorrect(cpsServeCotrReceived.getIsTheTimeEstimateCorrect())
                .withIsTheTimeEstimateCorrectDetails(cpsServeCotrReceived.getIsTheTimeEstimateCorrectDetails())
                .withLastRecordedTimeEstimate(cpsServeCotrReceived.getLastRecordedTimeEstimate())
                .withTag(cpsServeCotrReceived.getTag())
                .withTrialDate(cpsServeCotrReceived.getTrialDate());

        return cpsCotrReceivedDetails.build();
    }

    private final List<DefendantSubject> convertDefendantSubjects(final CpsServeCotrReceived cpsServeCotrReceived) {
        final List<DefendantSubject> defendantSubjectsList = new ArrayList<>();
        cpsServeCotrReceived.getDefendantSubject().forEach(defendantSubject -> {
            final DefendantSubject.Builder defendantSubjectBuilder = DefendantSubject.defendantSubject()
                    .withMatchingId(randomUUID())
                    .withAsn(defendantSubject.getAsn())
                    .withCpsDefendantId(defendantSubject.getCpsDefendantId())
                    .withProsecutorDefendantId(defendantSubject.getProsecutorDefendantId());

            final Optional<ProsecutorPersonDefendantDetails> prosecutorPersonDefendantDetails = ofNullable(defendantSubject.getProsecutorPersonDefendantDetails());
            final boolean defendantDetailsPresent = prosecutorPersonDefendantDetails.isPresent();
            if (defendantDetailsPresent) {
                defendantSubjectBuilder
                        .withDateOfBirth(DateUtil.convertToLocalDate(prosecutorPersonDefendantDetails.get().getDateOfBirth()))
                        .withForename(prosecutorPersonDefendantDetails.get().getForename())
                        .withForename2(prosecutorPersonDefendantDetails.get().getForename2())
                        .withForename3(prosecutorPersonDefendantDetails.get().getForename3())
                        .withSurname(prosecutorPersonDefendantDetails.get().getSurname())
                        .withTitle(prosecutorPersonDefendantDetails.get().getTitle())
                        .withProsecutorDefendantId(prosecutorPersonDefendantDetails.get().getProsecutorDefendantId());
                if(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants() != null){
                    defendantSubjectBuilder.withLocalAuthorityDetailsForYouthDefendants(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
                }

                if(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants() != null){
                    defendantSubjectBuilder.withParentGuardianForYouthDefendants(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
                }
            }

            final CpsPersonDefendantDetails cpsPersonDefendantDetails = defendantSubject.getCpsPersonDefendantDetails();
            if (cpsPersonDefendantDetails != null) {
                final String dateOfBirth = cpsPersonDefendantDetails.getDateOfBirth();
                if (dateOfBirth != null){
                    defendantSubjectBuilder
                            .withDateOfBirth(DateUtil.convertToLocalDate(dateOfBirth));
                }
                defendantSubjectBuilder
                        .withForename(cpsPersonDefendantDetails.getForename())
                        .withForename2(cpsPersonDefendantDetails.getForename2())
                        .withForename3(cpsPersonDefendantDetails.getForename3())
                        .withSurname(cpsPersonDefendantDetails.getSurname())
                        .withTitle(cpsPersonDefendantDetails.getTitle())
                        .withCpsDefendantId(cpsPersonDefendantDetails.getCpsDefendantId());
                if(cpsPersonDefendantDetails.getLocalAuthorityDetailsForYouthDefendants() != null){
                    defendantSubjectBuilder.withLocalAuthorityDetailsForYouthDefendants(cpsPersonDefendantDetails.getLocalAuthorityDetailsForYouthDefendants());
                }

                if(cpsPersonDefendantDetails.getParentGuardianForYouthDefendants() != null){
                    defendantSubjectBuilder.withParentGuardianForYouthDefendants(cpsPersonDefendantDetails.getParentGuardianForYouthDefendants());
                }
            }

            final CpsOrganisationDefendantDetails cpsOrganisationDefendantDetails = defendantSubject.getCpsOrganisationDefendantDetails();
            if (cpsOrganisationDefendantDetails != null) {
                defendantSubjectBuilder
                        .withOrganisationName(cpsOrganisationDefendantDetails.getOrganisationName())
                        .withCpsDefendantId(cpsOrganisationDefendantDetails.getCpsDefendantId());
            }

            final ProsecutorOrganisationDefendantDetails prosecutorOrganisationDefendantDetails = defendantSubject.getProsecutorOrganisationDefendantDetails();
            if (prosecutorOrganisationDefendantDetails != null) {
                defendantSubjectBuilder
                        .withOrganisationName(prosecutorOrganisationDefendantDetails.getOrganisationName())
                        .withProsecutorDefendantId(prosecutorOrganisationDefendantDetails.getProsecutorDefendantId());
            }

            defendantSubjectsList.add(defendantSubjectBuilder.build());
        });

        return defendantSubjectsList;
    }
}
