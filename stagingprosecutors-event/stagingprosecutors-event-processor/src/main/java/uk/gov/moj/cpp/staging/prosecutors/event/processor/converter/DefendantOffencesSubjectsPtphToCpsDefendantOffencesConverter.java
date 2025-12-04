package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjectsPtph;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;

import java.util.Optional;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendant;

public class DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter implements Converter<DefendantOffencesSubjectsPtph, CpsDefendant> {

    @Override
    public CpsDefendant convert(final DefendantOffencesSubjectsPtph defendantOffencesSubjects) {
        final DefendantSubject defendantSubject = defendantOffencesSubjects.getDefendant();

        final CpsDefendant.Builder cpsDefendantBuilder = CpsDefendant.cpsDefendant()
                .withMatchingId(randomUUID())
                .withPrincipalCharges(defendantOffencesSubjects.getPrincipalCharges());

        ofNullable(defendantSubject.getCpsDefendantId()).ifPresent(cpsDefendantBuilder::withCpsDefendantId);
        ofNullable(defendantSubject.getAsn()).ifPresent(cpsDefendantBuilder::withAsn);
        ofNullable(defendantSubject.getProsecutorDefendantId()).ifPresent(cpsDefendantBuilder::withProsecutorDefendantId);

        final Optional<ProsecutorPersonDefendantDetails> prosecutorPersonDefendantDetails = ofNullable(defendantSubject.getProsecutorPersonDefendantDetails());
        final boolean defendantDetailsPresent = prosecutorPersonDefendantDetails.isPresent();
        if (defendantDetailsPresent) {
            cpsDefendantBuilder
                    .withDateOfBirth(DateUtil.convertToLocalDate(prosecutorPersonDefendantDetails.get().getDateOfBirth()))
                    .withForename(prosecutorPersonDefendantDetails.get().getForename())
                    .withForename2(prosecutorPersonDefendantDetails.get().getForename2())
                    .withForename3(prosecutorPersonDefendantDetails.get().getForename3())
                    .withSurname(prosecutorPersonDefendantDetails.get().getSurname())
                    .withTitle(prosecutorPersonDefendantDetails.get().getTitle())
                    .withProsecutorDefendantId(prosecutorPersonDefendantDetails.get().getProsecutorDefendantId());
            if(ofNullable(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                cpsDefendantBuilder.withLocalAuthorityDetailsForYouthDefendants(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
            }

            if(ofNullable(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                cpsDefendantBuilder.withParentGuardianForYouthDefendants(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
            }
        }

        final Optional<CpsPersonDefendantDetails> cpsPersonDefendantDetails = ofNullable(defendantSubject.getCpsPersonDefendantDetails());
        final boolean cpsPersonDefendantDetailsPresent = cpsPersonDefendantDetails.isPresent();
        if (cpsPersonDefendantDetailsPresent) {
            final Optional<String> dateOfBirth = ofNullable(cpsPersonDefendantDetails.get().getDateOfBirth());
            if (dateOfBirth.isPresent()){
                cpsDefendantBuilder
                        .withDateOfBirth(DateUtil.convertToLocalDate(dateOfBirth.get()));
            }
            cpsDefendantBuilder
                    .withForename(cpsPersonDefendantDetails.get().getForename())
                    .withForename2(cpsPersonDefendantDetails.get().getForename2())
                    .withForename3(cpsPersonDefendantDetails.get().getForename3())
                    .withSurname(cpsPersonDefendantDetails.get().getSurname())
                    .withTitle(cpsPersonDefendantDetails.get().getTitle())
                    .withCpsDefendantId(cpsPersonDefendantDetails.get().getCpsDefendantId());
            if(ofNullable(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()){
                cpsDefendantBuilder.withLocalAuthorityDetailsForYouthDefendants(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants());
            }

            if(ofNullable(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()){
                cpsDefendantBuilder.withParentGuardianForYouthDefendants(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants());
            }
        }

        final Optional<CpsOrganisationDefendantDetails> cpsOrganisationDefendantDetails = ofNullable(defendantSubject.getCpsOrganisationDefendantDetails());
        final boolean cpsOrganisationDefendantDetailsPresent = cpsOrganisationDefendantDetails.isPresent();
        if (cpsOrganisationDefendantDetailsPresent) {
            cpsDefendantBuilder
                    .withOrganisationName(cpsOrganisationDefendantDetails.get().getOrganisationName())
                    .withCpsDefendantId(cpsOrganisationDefendantDetails.get().getCpsDefendantId());
        }

        final Optional<ProsecutorOrganisationDefendantDetails> prosecutorOrganisationDefendantDetails = ofNullable(defendantSubject.getProsecutorOrganisationDefendantDetails());
        final boolean prosecutorOrganisationDefendantDetailsPresent = prosecutorOrganisationDefendantDetails.isPresent();
        if (prosecutorOrganisationDefendantDetailsPresent) {
            cpsDefendantBuilder
                    .withOrganisationName(prosecutorOrganisationDefendantDetails.get().getOrganisationName())
                    .withProsecutorDefendantId(prosecutorOrganisationDefendantDetails.get().getProsecutorDefendantId());
        }

        return cpsDefendantBuilder
                .build();
    }
}
