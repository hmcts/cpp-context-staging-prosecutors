package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsUpdateCotrReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsUpdateCotrReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.DefendantSubject;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class CpsUpdateCotrReceivedToCpsUpdateCotrReceivedDetailsConverter implements Converter<CpsUpdateCotrReceived, CpsUpdateCotrReceivedDetails> {

    @Override
    public CpsUpdateCotrReceivedDetails convert(final CpsUpdateCotrReceived cpsUpdateCotrReceived) {

        final CpsUpdateCotrReceivedDetails.Builder cpsUpdateCotrReceivedDetails = CpsUpdateCotrReceivedDetails.cpsUpdateCotrReceivedDetails();
        cpsUpdateCotrReceivedDetails.withSubmissionId(cpsUpdateCotrReceived.getSubmissionId())
                .withSubmissionStatus(SubmissionStatus.valueOf(cpsUpdateCotrReceived.getSubmissionStatus().name()))
                .withCotrId(cpsUpdateCotrReceived.getCotrId())
                .withProsecutionCaseSubject(CpsProsecutionCaseSubject.cpsProsecutionCaseSubject()
                        .withProsecutingAuthority(cpsUpdateCotrReceived.getProsecutionCaseSubject().getProsecutingAuthority())
                        .withUrn(cpsUpdateCotrReceived.getProsecutionCaseSubject().getUrn())
                        .build())
                .withDefendantSubject(convertDefendantSubjects(cpsUpdateCotrReceived))
                .withTrialDate(cpsUpdateCotrReceived.getTrialDate())
                .withCertifyThatTheProsecutionIsTrialReady(cpsUpdateCotrReceived.getCertifyThatTheProsecutionIsTrialReady())
                .withDate(cpsUpdateCotrReceived.getDate())
                .withFormCompletedOnBehalfOfProsecutionBy(cpsUpdateCotrReceived.getFormCompletedOnBehalfOfProsecutionBy())
                .withFurtherProsecutionInformationProvidedAfterCertification(cpsUpdateCotrReceived.getFurtherProsecutionInformationProvidedAfterCertification())
                .withTag(cpsUpdateCotrReceived.getTag());

        return cpsUpdateCotrReceivedDetails.build();
    }

    private final List<DefendantSubject> convertDefendantSubjects(final CpsUpdateCotrReceived cpsUpdateCotrReceived) {
        final List<DefendantSubject> defendantSubjectsList = new ArrayList<>();
        cpsUpdateCotrReceived.getDefendantSubject().forEach(defendantSubject -> {
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
                if(ofNullable(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                    defendantSubjectBuilder.withLocalAuthorityDetailsForYouthDefendants(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
                }

                if(ofNullable(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                    defendantSubjectBuilder.withParentGuardianForYouthDefendants(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
                }
            }

            final Optional<CpsPersonDefendantDetails> cpsPersonDefendantDetails = ofNullable(defendantSubject.getCpsPersonDefendantDetails());
            final boolean cpsPersonDefendantDetailsPresent = cpsPersonDefendantDetails.isPresent();
            if (cpsPersonDefendantDetailsPresent) {
                final Optional<String> dateOfBirth = ofNullable(cpsPersonDefendantDetails.get().getDateOfBirth());
                if (dateOfBirth.isPresent()) {
                    defendantSubjectBuilder
                            .withDateOfBirth(DateUtil.convertToLocalDate(dateOfBirth.get()));
                }
                defendantSubjectBuilder
                        .withForename(cpsPersonDefendantDetails.get().getForename())
                        .withForename2(cpsPersonDefendantDetails.get().getForename2())
                        .withForename3(cpsPersonDefendantDetails.get().getForename3())
                        .withSurname(cpsPersonDefendantDetails.get().getSurname())
                        .withTitle(cpsPersonDefendantDetails.get().getTitle())
                        .withCpsDefendantId(cpsPersonDefendantDetails.get().getCpsDefendantId());
                if(ofNullable(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                    defendantSubjectBuilder.withLocalAuthorityDetailsForYouthDefendants(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
                }

                if(ofNullable(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                    defendantSubjectBuilder.withParentGuardianForYouthDefendants(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
                }
            }

            final Optional<CpsOrganisationDefendantDetails> cpsOrganisationDefendantDetails = ofNullable(defendantSubject.getCpsOrganisationDefendantDetails());
            final boolean cpsOrganisationDefendantDetailsPresent = cpsOrganisationDefendantDetails.isPresent();
            if (cpsOrganisationDefendantDetailsPresent) {
                defendantSubjectBuilder
                        .withOrganisationName(cpsOrganisationDefendantDetails.get().getOrganisationName())
                        .withCpsDefendantId(cpsOrganisationDefendantDetails.get().getCpsDefendantId());
            }

            final Optional<ProsecutorOrganisationDefendantDetails> prosecutorOrganisationDefendantDetails = ofNullable(defendantSubject.getProsecutorOrganisationDefendantDetails());
            final boolean prosecutorOrganisationDefendantDetailsPresent = prosecutorOrganisationDefendantDetails.isPresent();
            if (prosecutorOrganisationDefendantDetailsPresent) {
                defendantSubjectBuilder
                        .withOrganisationName(prosecutorOrganisationDefendantDetails.get().getOrganisationName())
                        .withProsecutorDefendantId(prosecutorOrganisationDefendantDetails.get().getProsecutorDefendantId());
            }

            defendantSubjectsList.add(defendantSubjectBuilder.build());
        });

        return defendantSubjectsList;
    }
}
