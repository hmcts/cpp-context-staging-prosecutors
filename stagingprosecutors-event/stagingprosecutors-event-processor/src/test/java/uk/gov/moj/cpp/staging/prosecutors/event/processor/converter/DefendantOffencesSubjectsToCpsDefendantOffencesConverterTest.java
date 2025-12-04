package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;


import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.DateUtil;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePetReceived;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendantOffences;
import org.junit.jupiter.api.Test;

public class DefendantOffencesSubjectsToCpsDefendantOffencesConverterTest {

    private static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    private static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ASN = "asn";
    private static final String FORE_NAME = "forename";
    private static final String SUR_NAME = "surname";
    private static final String TITLE = "title";
    private static final String OFFENCE_CODE = "offenceCode";
    private static final String OFFENCE_WORDING = "offenceWording";
    public static final String OFFENCE_DATE = "2021-09-27";

    private final FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(nullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }

    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenAsn_CpsDefendantId_ProsecutorId_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-asn-cps-prosecutor-ids.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getAsn(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getAsn(), is(ASN));

        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(CPS_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getForename(), is(nullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(nullValue()));
        assertThat(cpsDefendantOffence.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendantOffence.getTitle(), is(nullValue()));

        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenCpsDefendantDetails_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-cps-defendant-details.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getAsn(), is(nullValue()));

        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(CPS_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(nullValue()));

        assertThat(cpsDefendantOffence.getForename(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getForename(), is(FORE_NAME));

        assertThat(cpsDefendantOffence.getSurname(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(SUR_NAME));

        assertThat(cpsDefendantOffence.getDateOfBirth(), is(notNullValue()));

        assertThat(cpsDefendantOffence.getTitle(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getTitle(), is(TITLE));

        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));

        assertThat(cpsDefendantOffence.getLocalAuthorityDetailsForYouthDefendants(), is(notNullValue()));
    }

    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenCpsDefendantMandatoryDetails_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-cps-defendant-details-mandatory-fields-only.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(CPS_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getForename(), is(nullValue()));

        assertThat(cpsDefendantOffence.getSurname(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(SUR_NAME));

        assertThat(cpsDefendantOffence.getDateOfBirth(), is(nullValue()));
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenCpsOrganisationDetails_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-cps-organisation-details.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getAsn(), is(nullValue()));

        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(CPS_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getOrganisationName(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getOrganisationName(), is(ORGANISATION_NAME));

        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(nullValue()));
        assertThat(cpsDefendantOffence.getForename(), is(nullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(nullValue()));
        assertThat(cpsDefendantOffence.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendantOffence.getTitle(), is(nullValue()));

        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenProsecutorDefendantDetails_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-prosecutor-defendant-details.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getAsn(), is(nullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(nullValue()));

        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getForename(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getForename(), is(FORE_NAME));

        assertThat(cpsDefendantOffence.getSurname(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(SUR_NAME));

        assertThat(cpsDefendantOffence.getDateOfBirth(), is(notNullValue()));

        assertThat(cpsDefendantOffence.getTitle(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getTitle(), is(TITLE));

        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenProsecutorOrganisationDetails_present() throws IOException {
        final CpsServePetReceived cpsServePetReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-pet-received-prosecutor-organisation-details.json", CpsServePetReceived.class);
        final CpsDefendantOffences cpsDefendantOffence = new DefendantOffencesSubjectsToCpsDefendantOffencesConverter().convert(cpsServePetReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendantOffence.getAsn(), is(nullValue()));
        assertThat(cpsDefendantOffence.getCpsDefendantId(), is(nullValue()));

        assertThat(cpsDefendantOffence.getOrganisationName(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getOrganisationName(), is(ORGANISATION_NAME));

        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(notNullValue()));
        assertThat(cpsDefendantOffence.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));

        assertThat(cpsDefendantOffence.getForename(), is(nullValue()));
        assertThat(cpsDefendantOffence.getSurname(), is(nullValue()));
        assertThat(cpsDefendantOffence.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendantOffence.getTitle(), is(nullValue()));

        assertThat(cpsDefendantOffence.getMatchingId(), notNullValue());
        assertThat(cpsDefendantOffence.getCpsOffenceDetails(), hasSize(1));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getCjsOffenceCode(),is(OFFENCE_CODE));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceDate(),is(DateUtil.convertToLocalDate(OFFENCE_DATE)));
        assertThat(cpsDefendantOffence.getCpsOffenceDetails().get(0).getOffenceWording(),is(OFFENCE_WORDING));
    }

}
