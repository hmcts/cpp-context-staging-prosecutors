package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocxToCourtApplicationConverterTest {

    @Test
    public void shouldParseDocxFileWithSomeFields() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-new.docx").getFile()));

        final Map<String, String> result = DocxToCourtApplicationConverter.parse(docXInputStream);

        assertThat(result.size(), is(6));
        assertThat(result.get("applicant-first-name"), is("ALI"));
        assertThat(result.get("applicant-last-name"), is("YUKSEL"));
        assertThat(result.get("applicant-address-postcode"), is("E14 9XA"));
        assertThat(result.get("applicant-email-address"), nullValue());
    }

    @Test
    public void shouldConvertSubmitApplicationFromDocxFileForIndividualRespondent() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-individual-respondent-fields.docx").getFile()));

        final Map<String, String> result = DocxToCourtApplicationConverter.parse(docXInputStream);

        assertThat(result.size(), is(54));
        result.forEach((key, value) -> assertThat(value, is (key + "-value")) );

        final CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType().build();
        final SubmitApplication submitApplication = DocxToCourtApplicationConverter.prepareSubmitApplication(result, courtApplicationType, LocalDate.now());

        assertThat(submitApplication.getCourtApplication().getId(), is(not(nullValue())));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationType(), is(courtApplicationType));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getCaseURN(), is("related-case-urn-value"));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getProsecutorOuCode(), is("related-case-prosecutor-ou-code-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getIsSubject(), is(false));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisation().getName(), is("applicant-organisation-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getFirstName(), is("applicant-first-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getLastName(), is("applicant-last-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress1(), is("applicant-address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress2(), is("applicant-address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress3(), is("applicant-address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress4(), is("applicant-address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress5(), is("applicant-address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getPostcode(), is("applicant-address-postcode-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getWork(), is("applicant-contact-number-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getPrimaryEmail(), is("applicant-email-address-value"));

        verifyIndividualRespondents(submitApplication,0, "respondent-");
        verifyIndividualRespondents(submitApplication,1, "respondent2-");
        verifyIndividualRespondents(submitApplication,2, "respondent3-");
        verifyIndividualRespondents(submitApplication,3, "respondent4-");

        assertThat(submitApplication.getBoxHearingRequest().getCourtCentre().getName(), is("court-name-value"));

    }

    @Test
    public void shouldConvertSubmitApplicationFromDocxFileForOrganisationRespondent() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream docXInputStream = Files.newInputStream(Paths.get(classLoader.getResource("docx/iw018-eng-organisation-respondent-fields.docx").getFile()));

        final Map<String, String> result = DocxToCourtApplicationConverter.parse(docXInputStream);

        assertThat(result.size(), is(50));
        result.forEach((key, value) -> assertThat(value, is (key + "-value")) );

        final CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType().build();
        final SubmitApplication submitApplication = DocxToCourtApplicationConverter.prepareSubmitApplication(result, courtApplicationType, LocalDate.now());

        assertThat(submitApplication.getCourtApplication().getId(), is(not(nullValue())));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationType(), is(courtApplicationType));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getCaseURN(), is("related-case-urn-value"));
        assertThat(submitApplication.getCourtApplication().getCourtApplicationCases().get(0).getProsecutorOuCode(), is("related-case-prosecutor-ou-code-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getIsSubject(), is(false));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisation().getName(), is("applicant-organisation-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getFirstName(), is("applicant-first-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getLastName(), is("applicant-last-name-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress1(), is("applicant-address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress2(), is("applicant-address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress3(), is("applicant-address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress4(), is("applicant-address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getAddress5(), is("applicant-address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getAddress().getPostcode(), is("applicant-address-postcode-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getWork(), is("applicant-contact-number-value"));
        assertThat(submitApplication.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getContact().getPrimaryEmail(), is("applicant-email-address-value"));

        verifyOrganisationRespondents(submitApplication,0, "respondent-");
        verifyOrganisationRespondents(submitApplication,1, "respondent2-");
        verifyOrganisationRespondents(submitApplication,2, "respondent3-");
        verifyOrganisationRespondents(submitApplication,3, "respondent4-");

        assertThat(submitApplication.getBoxHearingRequest().getCourtCentre().getName(), is("court-name-value"));

    }

    private void verifyIndividualRespondents(final SubmitApplication submitApplication,int index,  final String prefix) {
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getAsn(), is(prefix+"asn-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getIsSubject(), is(index == 0 ));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getCpsDefendantId(), is(prefix+"cps-defendant-id-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getFirstName(), is(prefix+"first-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getLastName(), is(prefix+"last-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress1(), is(prefix+"address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress2(), is(prefix+"address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress3(), is(prefix+"address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress4(), is(prefix+"address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getAddress5(), is(prefix+"address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getPersonDetails().getAddress().getPostcode(), is(prefix+"address-postcode-value"));
    }

    private void verifyOrganisationRespondents(final SubmitApplication submitApplication, int index, final String prefix) {
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getAsn(), is(prefix+"asn-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getIsSubject(), is(index == 0 ));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getCpsDefendantId(), is(prefix+"cps-defendant-id-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getName(), is(prefix+"organisation-name-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress1(), is(prefix+"address-line1-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress2(), is(prefix+"address-line2-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress3(), is(prefix+"address-line3-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress4(), is(prefix+"address-line4-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getAddress5(), is(prefix+"address-line5-value"));
        assertThat(submitApplication.getCourtApplication().getRespondents().get(index).getOrganisation().getAddress().getPostcode(), is(prefix+"address-postcode-value"));
    }

}
