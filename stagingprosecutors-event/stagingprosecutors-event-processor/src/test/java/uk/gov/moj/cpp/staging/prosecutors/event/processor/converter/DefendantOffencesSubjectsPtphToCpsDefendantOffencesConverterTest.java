package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsServePtphReceived;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsDefendant;
import org.junit.jupiter.api.Test;

public class DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverterTest {

    private static final String PROSECUTOR_DEFENDANT_ID = "prosecutorDefendantId";
    private static final String CPS_DEFENDANT_ID = "cps-defendant-id";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ASN = "ASN";
    private static final String FORE_NAME = "forename";
    private static final String FORE_NAME2 = "forename2";
    private static final String FORE_NAME3 = "forename3";
    private static final String SUR_NAME = "surname";
    private static final String TITLE = "title";

    private final FileResourceObjectMapper fileResourceObjectMapper = new FileResourceObjectMapper();

    @Test
    public void shouldConvertDefendantOffencesSubjectsPtphToCpsDefendantOffences() throws IOException {
        final CpsServePtphReceived cpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received.json", CpsServePtphReceived.class);
        final CpsDefendant cpsDefendant = new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(cpsServePtphReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendant.getAsn(), is(ASN));
        assertThat(cpsDefendant.getCpsDefendantId(), is(CPS_DEFENDANT_ID));
        assertThat(cpsDefendant.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());

        assertThat(cpsDefendant.getForename(), is(nullValue()));
        assertThat(cpsDefendant.getSurname(), is(nullValue()));
        assertThat(cpsDefendant.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendant.getTitle(), is(nullValue()));
    }

    @Test
    public void shouldConvertDefendantOffencesSubjectsToCpsDefendantOffences_WhenCpsDefendantDetails_present() throws IOException {
        final CpsServePtphReceived cpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received-cps-person-defendant-details.json", CpsServePtphReceived.class);
        final CpsDefendant cpsDefendant = new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(cpsServePtphReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendant.getForename(), is(FORE_NAME));
        assertThat(cpsDefendant.getForename2(), is(FORE_NAME2));
        assertThat(cpsDefendant.getForename3(), is(FORE_NAME3));
        assertThat(cpsDefendant.getSurname(), is(SUR_NAME));
        assertThat(cpsDefendant.getDateOfBirth(), is(notNullValue()));
        assertThat(cpsDefendant.getTitle(), is(TITLE));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsPtphToCpsDefendantOffences_WhenProsecutorDefendantDetails_present() throws IOException {
        final CpsServePtphReceived cpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received-prosecutor-person-defendant-details.json", CpsServePtphReceived.class);
        final CpsDefendant cpsDefendant = new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(cpsServePtphReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendant.getForename(), is(FORE_NAME));
        assertThat(cpsDefendant.getForename2(), is(FORE_NAME2));
        assertThat(cpsDefendant.getForename3(), is(FORE_NAME3));
        assertThat(cpsDefendant.getSurname(), is(SUR_NAME));
        assertThat(cpsDefendant.getDateOfBirth(), is(notNullValue()));
        assertThat(cpsDefendant.getTitle(), is(TITLE));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());
    }

    @Test
    public void shouldConvertDefendantOffencesSubjectsPtphToCpsDefendantOffences_WhenCpsOrganisationDetails_present() throws IOException {
        final CpsServePtphReceived cpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received-cps-organisation-details.json", CpsServePtphReceived.class);
        final CpsDefendant cpsDefendant = new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(cpsServePtphReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendant.getOrganisationName(), is(ORGANISATION_NAME));
        assertThat(cpsDefendant.getCpsDefendantId(), is(CPS_DEFENDANT_ID));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());

        assertThat(cpsDefendant.getForename(), is(nullValue()));
        assertThat(cpsDefendant.getSurname(), is(nullValue()));
        assertThat(cpsDefendant.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendant.getTitle(), is(nullValue()));
    }


    @Test
    public void shouldConvertDefendantOffencesSubjectsPtphToCpsDefendantOffences_WhenProsecutorOrganisationDetails_present() throws IOException {
        final CpsServePtphReceived cpsServePtphReceived = fileResourceObjectMapper.convertFromFile("json/cps-serve-ptph-received-prosecutor-organisation-details.json", CpsServePtphReceived.class);
        final CpsDefendant cpsDefendant = new DefendantOffencesSubjectsPtphToCpsDefendantOffencesConverter().convert(cpsServePtphReceived.getDefendantOffencesSubjects().get(0));

        assertThat(cpsDefendant.getOrganisationName(), is(ORGANISATION_NAME));
        assertThat(cpsDefendant.getProsecutorDefendantId(), is(PROSECUTOR_DEFENDANT_ID));
        assertThat(cpsDefendant.getMatchingId(), notNullValue());

        assertThat(cpsDefendant.getForename(), is(nullValue()));
        assertThat(cpsDefendant.getSurname(), is(nullValue()));
        assertThat(cpsDefendant.getDateOfBirth(), is(nullValue()));
        assertThat(cpsDefendant.getTitle(), is(nullValue()));
    }

}
