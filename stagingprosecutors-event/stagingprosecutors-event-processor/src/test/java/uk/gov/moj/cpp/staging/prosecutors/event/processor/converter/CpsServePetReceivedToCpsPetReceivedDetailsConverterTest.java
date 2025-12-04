package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ApplicationsForDirectionsGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsPetReceivedDetails;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defence;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Prosecution;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup;
import org.junit.jupiter.api.Test;

public class CpsServePetReceivedToCpsPetReceivedDetailsConverterTest {

    private static final String MEASURE_DETAILS = "measureDetails";
    private static final String N = "N";
    private static final String Y = "Y";
    private static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final String OFFENCE_DATE = "2021-09-27";
    private static final String PROSECUTION_AUTHORITY = "The OU Code of the prosecuting authority";
    private static final String URN = "caseURN";
    private static final String NO_COMPLIANCE_DETAILS = "noComplianceDetails";
    private static final String DETAILS = "Details";
    private static final String SLAVERY_DETAILS = "SlaveryDetails";
    private static final String EQUIPMENT_DETAILS = "EquipmentDetails";
    private static final String LAW_DETAILS = "LawDetails";
    private static final String FORE_NAME = "forename";
    private static final String YES = "yes";
    private static final String WELSH = "welsh";
    private static final String LAST_NAME = "abc";
    public static final String DIRECTION_DETAILS = "directionDetails";
    public static final String PHONE = "8778888345";
    public static final String EMAIL = "test@test.com";
    private static final String NAME = "name";

    private final FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();
    

    @Test
    public void shouldConvertCpsServePetReceivedToCpsPetReceivedDetails() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received.json", CpsServePetReceived.class);
        final CpsPetReceivedDetails cpsPetReceivedDetails = new CpsServePetReceivedToCpsPetReceivedDetailsConverter().convert(cpsServePetReceived);
        assertCpsPetReceivedDetailsObject(cpsPetReceivedDetails, cpsServePetReceived.getSubmissionId());
    }


    private void assertCpsPetReceivedDetailsObject(final CpsPetReceivedDetails cpsPetReceivedDetails, final UUID submissionId) {
        assertThat(cpsPetReceivedDetails, notNullValue());
        assertThat(cpsPetReceivedDetails.getCpsDefendantOffences() , notNullValue());
        final List<CpsDefendantOffences> cpsDefendantOffencesList = cpsPetReceivedDetails.getCpsDefendantOffences();
        assertThat(cpsDefendantOffencesList, hasSize(1));
        cpsDefendantOffencesList.forEach(this::verifyCpsDefendantOffenceObject);

        assertThat(cpsPetReceivedDetails.getReviewingLawyer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getReviewingLawyer().getPhone(), is(PHONE));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getName(), is(NAME));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getEmail(), is(EMAIL));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseProgressionOfficer().getPhone(), is(PHONE));

        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject(), notNullValue());
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getProsecutingAuthority(), is(PROSECUTION_AUTHORITY));
        assertThat(cpsPetReceivedDetails.getProsecutionCaseSubject().getUrn(), is(URN));
        assertThat(cpsPetReceivedDetails.getSubmissionId(), is(submissionId));
        assertThat(cpsPetReceivedDetails.getSubmissionStatus().toString(), is(PENDING.toString()));

        assertThat(cpsPetReceivedDetails.getPetFormData() , notNullValue());
        assertThat(cpsPetReceivedDetails.getPetFormData().getDefence() ,is(notNullValue()));
        final Defence defence = cpsPetReceivedDetails.getPetFormData().getDefence();
        assertThat(defence.getDefendants() , hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsDefendantId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getProsecutorDefendantId() , is(PROSECUTOR_DEFENDANT_ID));
        assertThat(defence.getDefendants().get(0).getId() , is(nullValue()));
        assertThat(defence.getDefendants().get(0).getCpsOffences() , hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getOffenceCode() , is(OFFENCE_CODE));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getWording() , is(OFFENCE_WORDING));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getDate(), is(notNullValue()));

        assertThat(cpsPetReceivedDetails.getPetFormData().getProsecution() , is(notNullValue()));
        final Prosecution prosecution = cpsPetReceivedDetails.getPetFormData().getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach( witness -> verifyProsecutorWitnessObject(Y, witness));

        assertThat(prosecution.getDynamicFormAnswers() , is(notNullValue()));
        assertThat(prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup() , is(notNullValue()));
        final ApplicationsForDirectionsGroup applicationsForDirectionsGroup = prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup();
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutor() , is(Y));
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutorYesGroup().getVariationStandardDirectionsProsecutorYesGroupDetails() , is(DIRECTION_DETAILS));
        assertThat(applicationsForDirectionsGroup.getGroundRulesQuestioning(), is(Y));

        assertThat(prosecution.getDynamicFormAnswers().getProsecutorGroup() , is(notNullValue()));
        verifyProsecutorGroupObject(N, Y, prosecution);
    }

    private void verifyProsecutorGroupObject(final String no, final String yes, final Prosecution prosecution) {
        final ProsecutorGroup prosecutorGroup = prosecution.getDynamicFormAnswers().getProsecutorGroup();
        assertThat(prosecutorGroup.getProsecutorServeEvidence() , is(no));

        assertThat(prosecutorGroup.getProsecutionCompliance() , is(no));
        assertThat(prosecutorGroup.getProsecutionComplianceNoGroup().getProsecutionComplianceDetailsNo() , is(NO_COMPLIANCE_DETAILS));

        assertThat(prosecutorGroup.getPendingLinesOfEnquiry(), is(no));
        assertThat(prosecutorGroup.getPendingLinesOfEnquiryYesGroup().getPendingLinesOfEnquiryYesGroup(), is(DETAILS));

        assertThat(prosecutorGroup.getSlaveryOrExploitation(), is(no));
        assertThat(prosecutorGroup.getSlaveryOrExploitationYesGroup().getSlaveryOrExploitationDetails(), is(SLAVERY_DETAILS));

        assertThat(prosecutorGroup.getRelyOn().size(), is(12));
        assertThat(prosecutorGroup.getRelyOn().get(0).toString(), is("admissions"));

        assertThat(prosecutorGroup.getDisplayEquipment(), is(yes));
        assertThat(prosecutorGroup.getDisplayEquipmentYesGroup().getDisplayEquipmentDetails(), is(EQUIPMENT_DETAILS));

        assertThat(prosecutorGroup.getPointOfLaw(), is(yes));
        assertThat(prosecutorGroup.getPointOfLawYesGroup().getPointOfLawDetails(), is(LAW_DETAILS));
    }

    private void verifyProsecutorWitnessObject(final String yes, final cpp.moj.gov.uk.staging.prosecutors.json.schemas.Witnesses witness) {
        assertThat(witness.getAge(), is(14));
        assertThat(witness.getFirstName(), is(FORE_NAME));
        assertThat(witness.getInterpreterRequired(), is(yes));
        assertThat(witness.getLanguageAndDialect(), is(WELSH));
        assertThat(witness.getLastName(), is(LAST_NAME));
        assertThat(witness.getMeasuresRequired(), is(Arrays.asList(MEASURE_DETAILS)));
        assertThat(witness.getProsecutionProposesWitnessAttendInPerson(), is(yes));
        assertThat(witness.getSpecialOtherMeasuresRequired(), is(yes));
    }

    private void verifyCpsDefendantOffenceObject(final CpsDefendantOffences cpsDefendantOffence) {
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(nullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }
}
