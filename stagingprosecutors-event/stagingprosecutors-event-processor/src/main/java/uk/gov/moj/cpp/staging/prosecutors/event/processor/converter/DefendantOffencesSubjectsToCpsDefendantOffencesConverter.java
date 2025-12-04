package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;

public class DefendantOffencesSubjectsToCpsDefendantOffencesConverter implements Converter<DefendantOffencesSubjects, CpsDefendantOffences> {

    @Override
    public CpsDefendantOffences convert(final DefendantOffencesSubjects defendantOffencesSubjects) {
        final DefendantSubject defendantSubject = defendantOffencesSubjects.getDefendant();
        final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject> offenceSubjectList = defendantOffencesSubjects.getOffences();


        final CpsDefendantOffences.Builder cpsDefendantOffencesBuilder = CpsDefendantOffences.cpsDefendantOffences()
                .withMatchingId(randomUUID());
        ofNullable(defendantSubject.getCpsDefendantId()).ifPresent(cpsDefendantOffencesBuilder::withCpsDefendantId);
        ofNullable(defendantSubject.getAsn()).ifPresent(cpsDefendantOffencesBuilder::withAsn);
        ofNullable(defendantSubject.getProsecutorDefendantId()).ifPresent(cpsDefendantOffencesBuilder::withProsecutorDefendantId);

        final Optional<ProsecutorPersonDefendantDetails> prosecutorPersonDefendantDetails = ofNullable(defendantSubject.getProsecutorPersonDefendantDetails());
        final boolean defendantDetailsPresent = prosecutorPersonDefendantDetails.isPresent();
        if (defendantDetailsPresent) {
            cpsDefendantOffencesBuilder
                    .withDateOfBirth(DateUtil.convertToLocalDate(prosecutorPersonDefendantDetails.get().getDateOfBirth()))
                    .withForename(prosecutorPersonDefendantDetails.get().getForename())
                    .withForename2(prosecutorPersonDefendantDetails.get().getForename2())
                    .withForename3(prosecutorPersonDefendantDetails.get().getForename3())
                    .withSurname(prosecutorPersonDefendantDetails.get().getSurname())
                    .withTitle(prosecutorPersonDefendantDetails.get().getTitle())
                    .withProsecutorDefendantId(prosecutorPersonDefendantDetails.get().getProsecutorDefendantId());
            if(ofNullable(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                cpsDefendantOffencesBuilder.withLocalAuthorityDetailsForYouthDefendants(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
            }

            if(ofNullable(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                cpsDefendantOffencesBuilder.withParentGuardianForYouthDefendants(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
            }
        }

        final Optional<CpsPersonDefendantDetails> cpsPersonDefendantDetails = ofNullable(defendantSubject.getCpsPersonDefendantDetails());
        final boolean cpsPersonDefendantDetailsPresent = cpsPersonDefendantDetails.isPresent();
        if (cpsPersonDefendantDetailsPresent) {
            final Optional<String> dateOfBirth = ofNullable(cpsPersonDefendantDetails.get().getDateOfBirth());
            if (dateOfBirth.isPresent()){
                cpsDefendantOffencesBuilder
                        .withDateOfBirth(DateUtil.convertToLocalDate(dateOfBirth.get()));
            }
            cpsDefendantOffencesBuilder
                    .withForename(cpsPersonDefendantDetails.get().getForename())
                    .withForename2(cpsPersonDefendantDetails.get().getForename2())
                    .withForename3(cpsPersonDefendantDetails.get().getForename3())
                    .withSurname(cpsPersonDefendantDetails.get().getSurname())
                    .withTitle(cpsPersonDefendantDetails.get().getTitle())
                    .withCpsDefendantId(cpsPersonDefendantDetails.get().getCpsDefendantId());
            if(ofNullable(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                cpsDefendantOffencesBuilder.withLocalAuthorityDetailsForYouthDefendants(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
            }

            if(ofNullable(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                cpsDefendantOffencesBuilder.withParentGuardianForYouthDefendants(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
            }
        }

        final Optional<CpsOrganisationDefendantDetails> cpsOrganisationDefendantDetails = ofNullable(defendantSubject.getCpsOrganisationDefendantDetails());
        final boolean cpsOrganisationDefendantDetailsPresent = cpsOrganisationDefendantDetails.isPresent();
        if (cpsOrganisationDefendantDetailsPresent) {
            cpsDefendantOffencesBuilder
                    .withOrganisationName(cpsOrganisationDefendantDetails.get().getOrganisationName())
                    .withCpsDefendantId(cpsOrganisationDefendantDetails.get().getCpsDefendantId());
        }

        final Optional<ProsecutorOrganisationDefendantDetails> prosecutorOrganisationDefendantDetails = ofNullable(defendantSubject.getProsecutorOrganisationDefendantDetails());
        final boolean prosecutorOrganisationDefendantDetailsPresent = prosecutorOrganisationDefendantDetails.isPresent();
        if (prosecutorOrganisationDefendantDetailsPresent) {
            cpsDefendantOffencesBuilder
                    .withOrganisationName(prosecutorOrganisationDefendantDetails.get().getOrganisationName())
                    .withProsecutorDefendantId(prosecutorOrganisationDefendantDetails.get().getProsecutorDefendantId());
        }

        return cpsDefendantOffencesBuilder
                .withCpsOffenceDetails(buildCpsOffenceDetailsList(offenceSubjectList))
                .build();
    }

    private List<cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsOffenceDetails> buildCpsOffenceDetailsList(final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceSubject> offenceSubjectList) {
        return offenceSubjectList.stream().map(offenceSubject -> cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsOffenceDetails.cpsOffenceDetails()
                .withCjsOffenceCode(offenceSubject.getCjsOffenceCode())
                .withOffenceWording(offenceSubject.getOffenceWording())
                .withOffenceDate(DateUtil.convertToLocalDate(offenceSubject.getOffenceDate()))
                .build())
                .collect(Collectors.toList());
    }
}
