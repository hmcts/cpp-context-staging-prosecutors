package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup.prosecutorGroup;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil.convertToLocalDate;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.LocalAuthorityDetailsForYouthDefendants;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionWillRelyOn;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.RelyOn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ApplicationsForDirectionsGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.AssociatedPerson;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.AuthorityDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defence;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.DisplayEquipmentYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.DynamicFormAnswers;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.GuardianDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.PendingLinesOfEnquiryYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.PetFormData;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.PointOfLawYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Prosecution;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutionComplianceNoGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutionComplianceYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorServeEvidenceYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SlaveryOrExploitationYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.VariationStandardDirectionsProsecutorYesGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Witnesses;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S3776", "squid:S1188","squid:S3655"})
public class CpsServePetReceivedToPetFormDataConverter implements Converter<CpsServePetReceived, PetFormData> {

    @Override
    public PetFormData convert(final CpsServePetReceived cpsServePetReceived) {
        return PetFormData.petFormData()
                .withDefence(Defence.defence().
                        withDefendants(buildDefendantList(cpsServePetReceived.getDefendantOffencesSubjects()))
                        .build())
                .withProsecution(buildProsecution(cpsServePetReceived))
                .build();
    }

    private List<cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defendants> buildDefendantList(final List<DefendantOffencesSubjects> defendantOffencesSubjectsList) {
        final List<cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defendants> defendants = new ArrayList<>();

        defendantOffencesSubjectsList.forEach(defendantOffencesSubject -> {
            final cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defendants.Builder builder = cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defendants.defendants()
                    .withCpsDefendantId(defendantOffencesSubject.getDefendant().getCpsDefendantId())
                    .withProsecutorDefendantId(defendantOffencesSubject.getDefendant().getProsecutorDefendantId())
                    .withCpsOffences(buildCpsOffenceListFromDefendantOffenceSubject(defendantOffencesSubject))
                    .withAssociatedPerson(buildAssociatedPerson(defendantOffencesSubject.getDefendant()));

            defendants.add(builder.build());
        });

        return defendants;
    }

    private AssociatedPerson buildAssociatedPerson(DefendantSubject defendantSubject) {
        final Optional<ProsecutorPersonDefendantDetails> prosecutorPersonDefendantDetails = ofNullable(defendantSubject.getProsecutorPersonDefendantDetails());
        final boolean defendantDetailsPresent = prosecutorPersonDefendantDetails.isPresent();

        final Optional<CpsPersonDefendantDetails> cpsPersonDefendantDetails = ofNullable(defendantSubject.getCpsPersonDefendantDetails());
        final boolean cpsPersonDefendantDetailsPresent = cpsPersonDefendantDetails.isPresent();

        final AssociatedPerson.Builder builder = AssociatedPerson.associatedPerson();

        if (defendantDetailsPresent) {
            if (ofNullable(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()) {
                final LocalAuthorityDetailsForYouthDefendants localAuthorityDetailsForYouthDefendants = ofNullable(prosecutorPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).get();
                builder.withAuthorityDetails(AuthorityDetails.authorityDetails()
                        .withName(convertName(localAuthorityDetailsForYouthDefendants))
                        .withAddress(convertAuthorityAddress(localAuthorityDetailsForYouthDefendants))
                        .withEmail(localAuthorityDetailsForYouthDefendants.getEmail())
                        .withPhone(localAuthorityDetailsForYouthDefendants.getPhone())
                        .withRef(localAuthorityDetailsForYouthDefendants.getReference())
                        .withResponsible(localAuthorityDetailsForYouthDefendants.getAuthority())
                        .build());
            }

            if (ofNullable(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()) {
                final DefendantParentGuardian defendantParentGuardian = ofNullable(prosecutorPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).get();

                builder.withGuardianDetails(GuardianDetails.guardianDetails()
                        .withName(convertGuardianName(defendantParentGuardian))
                        .withAddress(convertGuardianAddress(defendantParentGuardian))
                        .withEmail(defendantParentGuardian.getEmail())
                        .withRelationship(defendantParentGuardian.getRelationshipToDefendant())
                        .build());
            }

            if (cpsPersonDefendantDetailsPresent) {
                if (ofNullable(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).isPresent()) {
                    final LocalAuthorityDetailsForYouthDefendants localAuthorityDetailsForYouthDefendants = ofNullable(cpsPersonDefendantDetails.get().getLocalAuthorityDetailsForYouthDefendants()).get();
                    builder.withAuthorityDetails(AuthorityDetails.authorityDetails()
                            .withName(convertName(localAuthorityDetailsForYouthDefendants))
                            .withAddress(convertAuthorityAddress(localAuthorityDetailsForYouthDefendants))
                            .withEmail(localAuthorityDetailsForYouthDefendants.getEmail())
                            .withPhone(localAuthorityDetailsForYouthDefendants.getPhone())
                            .withRef(localAuthorityDetailsForYouthDefendants.getReference())
                            .withResponsible(localAuthorityDetailsForYouthDefendants.getAuthority())
                            .build());
                }

                if (ofNullable(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).isPresent()) {
                    final DefendantParentGuardian defendantParentGuardian = ofNullable(cpsPersonDefendantDetails.get().getParentGuardianForYouthDefendants()).get();

                    builder.withGuardianDetails(GuardianDetails.guardianDetails()
                            .withName(convertGuardianName(defendantParentGuardian))
                            .withAddress(convertGuardianAddress(defendantParentGuardian))
                            .withEmail(defendantParentGuardian.getEmail())
                            .withRelationship(defendantParentGuardian.getRelationshipToDefendant())
                            .build());
                }
            }
        }
        return builder.build();
    }

    private String convertName(final LocalAuthorityDetailsForYouthDefendants localAuthorityDetailsForYouthDefendants) {
        String name = StringUtils.EMPTY;
        if (nonNull(localAuthorityDetailsForYouthDefendants.getForename())) {
            name = name.concat(localAuthorityDetailsForYouthDefendants.getForename());
        }

        if (nonNull(localAuthorityDetailsForYouthDefendants.getSurname())) {
            name = concatString(name);
            name = name.concat(localAuthorityDetailsForYouthDefendants.getSurname());
        }
        return name;
    }

    private String convertGuardianName(final DefendantParentGuardian defendantParentGuardian) {
        String guardianName = StringUtils.EMPTY;
        if (nonNull(defendantParentGuardian.getForename())) {
            guardianName = guardianName.concat(defendantParentGuardian.getForename());
        }

        if (ofNullable(defendantParentGuardian.getForename2()).isPresent()) {
            guardianName = concatString(guardianName);
            guardianName = guardianName.concat(ofNullable(defendantParentGuardian.getForename2()).get());
        }

        if (ofNullable(defendantParentGuardian.getForename3()).isPresent()) {
            guardianName = concatString(guardianName);
            guardianName = guardianName.concat(ofNullable(defendantParentGuardian.getForename3()).get());
        }

        if (nonNull(defendantParentGuardian.getSurname())) {
            guardianName = concatString(guardianName);
            guardianName = guardianName.concat(defendantParentGuardian.getSurname());
        }
        return guardianName;
    }

    private String concatString(String value){
        if(value.length()>0){
            value = value.concat(StringUtils.SPACE);
        }
        return value;
    }

    private String convertAuthorityAddress(final LocalAuthorityDetailsForYouthDefendants localAuthorityDetailsForYouthDefendants) {
        String address = StringUtils.EMPTY;

        if(nonNull(localAuthorityDetailsForYouthDefendants.getAddress1())){
            address = address.concat(localAuthorityDetailsForYouthDefendants.getAddress1());
        }

        if(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress2()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress2()).get());
        }

        if(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress3()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress3()).get());
        }

        if(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress4()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress4()).get());
        }

        if(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress5()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(localAuthorityDetailsForYouthDefendants.getAddress5()).get());
        }

        if(ofNullable(localAuthorityDetailsForYouthDefendants.getPostcode()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(localAuthorityDetailsForYouthDefendants.getPostcode()).get());
        }

        return address;
    }

    private String convertGuardianAddress(final DefendantParentGuardian defendantParentGuardian) {
        String address = StringUtils.EMPTY;

        if(nonNull(defendantParentGuardian.getAddress1())){
            address = address.concat(defendantParentGuardian.getAddress1());
        }

        if(ofNullable(defendantParentGuardian.getAddress2()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(defendantParentGuardian.getAddress2()).get());
        }

        if(ofNullable(defendantParentGuardian.getAddress3()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(defendantParentGuardian.getAddress3()).get());
        }

        if(ofNullable(defendantParentGuardian.getAddress4()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(defendantParentGuardian.getAddress4()).get());
        }

        if(ofNullable(defendantParentGuardian.getAddress5()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(defendantParentGuardian.getAddress5()).get());
        }

        if(ofNullable(defendantParentGuardian.getPostcode()).isPresent()){
            address = concatString(address);
            address = address.concat(ofNullable(defendantParentGuardian.getPostcode()).get());
        }

        return address;
    }

    private List<CpsOffences> buildCpsOffenceListFromDefendantOffenceSubject(final uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantOffencesSubjects defendantOffencesSubject) {
        return defendantOffencesSubject.getOffences().stream().map(offenceSubject -> CpsOffences.cpsOffences()
                .withOffenceCode(offenceSubject.getCjsOffenceCode())
                .withWording(offenceSubject.getOffenceWording())
                .withDate(convertToLocalDate(offenceSubject.getOffenceDate()))
                .build())
                .collect(toList());
    }

    private Prosecution buildProsecution(final CpsServePetReceived cpsServePetReceived) {
        return Prosecution.prosecution()
                .withDynamicFormAnswers(DynamicFormAnswers.dynamicFormAnswers()
                        .withApplicationsForDirectionsGroup(buildApplicationGroup(cpsServePetReceived))
                        .withProsecutorGroup(buildProsecutorGroup(cpsServePetReceived))
                        .withAdditionalInformation(cpsServePetReceived.getAdditionalInformation())
                        .build())
                .withWitnesses(extractWitnessesList(cpsServePetReceived.getWitnesses()))
                .build();
    }

    private List<Witnesses> extractWitnessesList(final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Witnesses> witnessesList) {
        if (CollectionUtils.isEmpty(witnessesList)) {
            return Collections.emptyList();
        }
        return witnessesList.stream()
                .map(this::getWitnesses)
                .collect(toList());
    }

    private Witnesses getWitnesses(final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Witnesses cpsWitness) {
        final Witnesses.Builder builder = Witnesses.witnesses()
                .withFirstName(cpsWitness.getForename())
                .withLanguageAndDialect(cpsWitness.getInterpreterLanguageDialect())
                .withLastName(cpsWitness.getSurname())
                .withSpecialOtherMeasuresRequired(cpsWitness.getProsecutionWitnessRequiringAssistanceSpecialMeasures())
                .withSelected(Boolean.TRUE)
                .withProsecutionProposesWitnessAttendInPerson(cpsWitness.getProsecutionRequireAttendance() ? "Y" : "N");

        ofNullable(cpsWitness.getAgeIfUnder18()).ifPresent(builder::withAge);

        if (cpsWitness.getWelshLanguage() && cpsWitness.getInterpreterRequired()) {
            builder.withInterpreterRequired("");
        } else if (cpsWitness.getWelshLanguage()) {
            builder.withInterpreterRequired("evidenceInWelsh");
        } else if (cpsWitness.getInterpreterRequired()) {
            builder.withInterpreterRequired("Y");
        } else {
            builder.withInterpreterRequired("N");
        }

        ofNullable(cpsWitness.getProsecutionWitnessRequiringAssistanceSpecialMeasuresDetails())
                .ifPresent(specialMeasures -> builder.withMeasuresRequired(singletonList(specialMeasures)));

        return builder.build();
    }


    private ApplicationsForDirectionsGroup buildApplicationGroup(final CpsServePetReceived cpsServePetReceived) {
        return ApplicationsForDirectionsGroup.applicationsForDirectionsGroup()
                .withGroundRulesQuestioning(cpsServePetReceived.getCourtToArrangeADiscussionOfGroundRulesForQuestioning())
                .withVariationStandardDirectionsProsecutor(cpsServePetReceived.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirection())
                .withVariationStandardDirectionsProsecutorYesGroup(VariationStandardDirectionsProsecutorYesGroup.variationStandardDirectionsProsecutorYesGroup()
                        .withVariationStandardDirectionsProsecutorYesGroupDetails(cpsServePetReceived.getVaryAStandardTrialPreparationTimeLimitOrMakeAnyOtherDirectionDetails())
                        .build())
                .build();
    }

    private ProsecutorGroup buildProsecutorGroup(final CpsServePetReceived cpsServePetReceived) {
        final ProsecutionComplianceNoGroup.Builder prosecutionComplianceNoGroupBuilder = ProsecutionComplianceNoGroup.prosecutionComplianceNoGroup();
        final ProsecutionComplianceYesGroup.Builder prosecutionComplianceYesGroupBuilder = ProsecutionComplianceYesGroup.prosecutionComplianceYesGroup();

        if ("Y".equalsIgnoreCase(cpsServePetReceived.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith())) {
            prosecutionComplianceYesGroupBuilder.withProsecutionComplianceDetailsYes(cpsServePetReceived.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas());
        }

        if ("N".equalsIgnoreCase(cpsServePetReceived.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith())) {
            prosecutionComplianceNoGroupBuilder.withProsecutionComplianceDetailsNo(cpsServePetReceived.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWithStateWhenThisWas());
        }

        final DisplayEquipmentYesGroup.Builder displayEquipmentYesGroupBuilder = DisplayEquipmentYesGroup.displayEquipmentYesGroup();
        ofNullable(cpsServePetReceived.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoomDetails()).ifPresent(displayEquipmentYesGroupBuilder::withDisplayEquipmentDetails);

        final PendingLinesOfEnquiryYesGroup.Builder pendingLinesOfEnquiryYesGroupBuilder = PendingLinesOfEnquiryYesGroup.pendingLinesOfEnquiryYesGroup();
        ofNullable(cpsServePetReceived.getAreThereAnyPendingEnquiriesOrLinesOfInvestigationDetails()).ifPresent(pendingLinesOfEnquiryYesGroupBuilder::withPendingLinesOfEnquiryYesGroup);

        return prosecutorGroup()
                .withDisplayEquipment(cpsServePetReceived.getWillTheProsecutorNeedAnyEquipmentInTheTrialCourtRoom())
                .withDisplayEquipmentYesGroup(displayEquipmentYesGroupBuilder.build())
                .withPendingLinesOfEnquiry(cpsServePetReceived.getAreThereAnyPendingEnquiriesOrLinesOfInvestigation())
                .withPendingLinesOfEnquiryYesGroup(pendingLinesOfEnquiryYesGroupBuilder.build())
                .withPointOfLaw(cpsServePetReceived.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFact())
                .withPointOfLawYesGroup(PointOfLawYesGroup.pointOfLawYesGroup()
                        .withPointOfLawDetails(cpsServePetReceived.getExpectTheCaseToInvolveAComplexNovelOrUnusualPointOfLawAndOrFactDetails())
                        .build())
                .withProsecutionCompliance(cpsServePetReceived.getHasTheInitialDutyOfDisclosureOfUnusedMaterialBeenCompliedWith())
                .withProsecutionComplianceNoGroup(prosecutionComplianceNoGroupBuilder.build())
                .withProsecutionComplianceYesGroup(prosecutionComplianceYesGroupBuilder.build())
                .withProsecutorServeEvidence(cpsServePetReceived.getDoesTheProsecutorIntendToServeMoreEvidence())
                .withProsecutorServeEvidenceYesGroup(ProsecutorServeEvidenceYesGroup.prosecutorServeEvidenceYesGroup()
                        .withProsecutorServeEvidenceDetails(cpsServePetReceived.getDoesTheProsecutorIntendToServeMoreEvidenceDetails())
                        .build())
                .withRelyOn(getRelyOnList(cpsServePetReceived.getProsecutionWillRelyOn()))
                .withSlaveryOrExploitation(cpsServePetReceived.getHasDefendantHasBeenAVictimOfSlaveryOrExploitation())
                .withSlaveryOrExploitationYesGroup(SlaveryOrExploitationYesGroup.slaveryOrExploitationYesGroup()
                        .withSlaveryOrExploitationDetails(cpsServePetReceived.getHasDefendantHasBeenAVictimOfSlaveryOrExploitationDetails())
                        .build())

                .build();
    }

    private static Map<ProsecutionWillRelyOn, RelyOn> createProsecutionWillRelyOnToRelyOnMap() {
        final EnumMap<ProsecutionWillRelyOn, RelyOn> prosecutionWillRelyOnToRelyOnMap = new EnumMap<>(ProsecutionWillRelyOn.class);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.A_RECORD_OF_THE_DEFENDANTS_INTERVIEW, RelyOn.RECORD);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.A_SUMMARY_OF_THE_DEFENDANT_INTERVIEW, RelyOn.SUMMARY);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.BAD_CHARACTER_EVIDENCE, RelyOn.BAD);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.DEFENDANT_ADMISSIONS_IN_INTERVIEW, RelyOn.ADMISSIONS);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.DEFENDANT_FAILURE_TO_MENTION_FACTS_IN_INTERVIEWS, RelyOn.FAILURE);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.CCTV_EVIDENCE, RelyOn.CCTV);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.DIAGRAM, RelyOn.DIAGRAM);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.ELECTRONICALLY_RECORDED_EVIDENCE, RelyOn.ELECTRONICALLY);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.EXPERT_EVIDENCE, RelyOn.EXPERT);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.HEARSAY_EVIDENCE, RelyOn.HEARSAY);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.PHOTOS, RelyOn.PHOTOS);
        prosecutionWillRelyOnToRelyOnMap.put(ProsecutionWillRelyOn.SKETCH_MAP, RelyOn.SKETCH);
        return prosecutionWillRelyOnToRelyOnMap;
    }

    private List<RelyOn> getRelyOnList(final List<ProsecutionWillRelyOn> prosecutionWillRelyOnList) {
        final Map<ProsecutionWillRelyOn, RelyOn> converterMap = createProsecutionWillRelyOnToRelyOnMap();
        final List<RelyOn> relyOnList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(prosecutionWillRelyOnList)) {
            prosecutionWillRelyOnList.forEach(prosecutionWillRelyOn -> relyOnList.add(converterMap.get(prosecutionWillRelyOn)));
        }
        return relyOnList;
    }
}
