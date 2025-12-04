package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.Arrays;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ApplicationsForDirectionsGroup;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Defence;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.PetFormData;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.Prosecution;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.ProsecutorGroup;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class CpsServePetReceivedToPetFormDataConverterTest {

    private static final String MEASURE_DETAILS = "measureDetails";
    private static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String OFFENCE_WORDING = "offenceWording";
    private static final String NO_COMPLIANCE_DETAILS = "noComplianceDetails";
    private static final String DETAILS = "Details";
    private static final String SLAVERY_DETAILS = "SlaveryDetails";
    private static final String EQUIPMENT_DETAILS = "EquipmentDetails";
    private static final String LAW_DETAILS = "LawDetails";
    private static final String FORE_NAME = "forename";
    private static final String WELSH = "welsh";
    private static final String LAST_NAME = "abc";
    public static final String DIRECTION_DETAILS = "directionDetails";

    private final FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertCpsServePetReceivedToPetFormData() throws IOException {
        CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received.json", CpsServePetReceived.class);
        PetFormData petFormData = new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived);
        assertPetFormDataObject(petFormData);
    }

    @Test
    public void shouldConvertCpsServePetReceivedToPetFormData_interpreterRequired_welsh() throws IOException {
        String no = "N";
        String yes = "Y";
        CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-with-interpreterRequired-welsh.json", CpsServePetReceived.class);
        PetFormData petFormData = new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived);
        assertThat(petFormData, notNullValue());
        assertThat(petFormData.getDefence(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> {
            assertThat(witness.getInterpreterRequired(), is("evidenceInWelsh"));
            assertThat(witness.getLanguageAndDialect(), is(nullValue()));
        });
    }


    @Test
    public void shouldConvertCpsServePetReceivedToPetFormData_interpreterRequired_true() throws IOException {
        String no = "N";
        String yes = "Y";
        CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-with-interpreterRequired-true.json", CpsServePetReceived.class);
        PetFormData petFormData = new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived);
        assertThat(petFormData, notNullValue());
        assertThat(petFormData.getDefence(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> {
            assertThat(witness.getInterpreterRequired(), is("Y"));
            assertThat(witness.getLanguageAndDialect(), is(notNullValue()));
            assertThat(witness.getLanguageAndDialect(), is("spanish"));
        });
    }

    @Test
    public void shouldConvertCpsServePetReceivedToPetFormData_interpreterRequired_false() throws IOException {
        String no = "N";
        String yes = "Y";
        CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-with-interpreterRequired-false.json", CpsServePetReceived.class);
        PetFormData petFormData = new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived);
        assertThat(petFormData, notNullValue());
        assertThat(petFormData.getDefence(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> {
            assertThat(witness.getInterpreterRequired(), is("N"));
            assertThat(witness.getLanguageAndDialect(), is(nullValue()));
        });
    }

    @Test
    public void shouldConvertCpsServePetReceivedToPetFormData_interpreterRequired_true_welsh_true() throws IOException {
        String no = "N";
        String yes = "Y";
        CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-with-interpreterRequired-true-welsh-true.json", CpsServePetReceived.class);
        PetFormData petFormData = new CpsServePetReceivedToPetFormDataConverter().convert(cpsServePetReceived);
        assertThat(petFormData, notNullValue());
        assertThat(petFormData.getDefence(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> {
            assertThat(witness.getInterpreterRequired(), is(StringUtils.EMPTY));
            assertThat(witness.getLanguageAndDialect(), is(notNullValue()));
            assertThat(witness.getLanguageAndDialect(), is("spanish"));
        });
    }

    private void assertPetFormDataObject(final PetFormData petFormData) {
        String no = "N";
        String yes = "Y";
        assertThat(petFormData, notNullValue());
        assertThat(petFormData.getDefence(), is(notNullValue()));
        Defence defence = petFormData.getDefence();
        assertThat(defence.getDefendants(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsDefendantId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(defence.getDefendants().get(0).getId(), is(nullValue()));
        assertThat(defence.getDefendants().get(0).getCpsOffences(), hasSize(1));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getOffenceCode(), is(OFFENCE_CODE));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getWording(), is(OFFENCE_WORDING));
        assertThat(defence.getDefendants().get(0).getCpsOffences().get(0).getDate(), is(notNullValue()));

        assertThat(petFormData.getProsecution(), is(notNullValue()));
        Prosecution prosecution = petFormData.getProsecution();

        assertThat(prosecution.getWitnesses(), hasSize(1));
        prosecution.getWitnesses().forEach(witness -> verifyProsecutorWitnessObject(yes, yes, WELSH, witness));

        assertThat(prosecution.getDynamicFormAnswers(), is(notNullValue()));
        assertThat(prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup(), is(notNullValue()));
        final ApplicationsForDirectionsGroup applicationsForDirectionsGroup = prosecution.getDynamicFormAnswers().getApplicationsForDirectionsGroup();
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutor(), is(yes));
        assertThat(applicationsForDirectionsGroup.getVariationStandardDirectionsProsecutorYesGroup().getVariationStandardDirectionsProsecutorYesGroupDetails(), is(DIRECTION_DETAILS));
        assertThat(applicationsForDirectionsGroup.getGroundRulesQuestioning(), is(yes));

        assertThat(prosecution.getDynamicFormAnswers().getProsecutorGroup(), is(notNullValue()));
        verifyProsecutorGroupObject(no, yes, prosecution);

    }

    private void verifyProsecutorGroupObject(final String no, final String yes, final Prosecution prosecution) {
        ProsecutorGroup prosecutorGroup = prosecution.getDynamicFormAnswers().getProsecutorGroup();
        assertThat(prosecutorGroup.getProsecutorServeEvidence(), is(no));

        assertThat(prosecutorGroup.getProsecutionCompliance(), is(no));
        assertThat(prosecutorGroup.getProsecutionComplianceNoGroup().getProsecutionComplianceDetailsNo(), is(NO_COMPLIANCE_DETAILS));

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

    private void verifyProsecutorWitnessObject(final String yes, String interpreterRequired, String languageAndDialect, cpp.moj.gov.uk.staging.prosecutors.json.schemas.Witnesses witness) {
        assertThat(witness.getAge(), is(14));
        assertThat(witness.getFirstName(), is(FORE_NAME));
        assertThat(witness.getInterpreterRequired(), is(interpreterRequired));
        assertThat(witness.getLanguageAndDialect(), is(languageAndDialect));
        assertThat(witness.getLastName(), is(LAST_NAME));
        assertThat(witness.getMeasuresRequired(), is(Arrays.asList(MEASURE_DETAILS)));
        assertThat(witness.getProsecutionProposesWitnessAttendInPerson(), is(yes));
        assertThat(witness.getSpecialOtherMeasuresRequired(), is(yes));
        assertThat(witness.getSelected(), is(notNullValue()));
        assertThat(witness.getSelected(), is(TRUE));
    }

}
