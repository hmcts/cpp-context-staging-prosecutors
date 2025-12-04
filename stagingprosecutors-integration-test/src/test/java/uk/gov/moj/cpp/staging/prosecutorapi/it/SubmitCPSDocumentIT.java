package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.extractSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmission;

import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.ResourcesUtils;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class SubmitCPSDocumentIT {

    public static final String CASE_LEVEL = "Case level";
    public static final String DEFENDANT_LEVEL = "Defendant level";

    private static final String CPS_DOCUMENT_UPLOAD_COMMAND_URL = getBaseUri() + "/stagingprosecutors-command-api/command/api/rest/stagingprosecutors/v1/prosecutions/cps-xml/materials";
    public static final String ERROR_MESSAGE_PREFIX = "Invalid content was found starting with element";
    private static final String INVALID_FIELD_NAME = "ResponseEmail";
    private static final String PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cases/%s/material";

    private static UUID CASE_ID = null;
    private static final WiremockUtils wiremockUtils = new WiremockUtils();
    private static String materialUploadUrl = null;

    @BeforeAll
    public static void setUp() {

        CASE_ID = randomUUID();
        materialUploadUrl = format(PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL, CASE_ID);

        wiremockUtils
                .stubPost(materialUploadUrl)
                .stubIdMapperReturningExistingAssociation(CASE_ID)
                .stubReferenceDataParentBundleSections("12", "stub-data/referencedata.get-parent-bundle-sections-case-level.json")
                .stubReferenceDataParentBundleSections("11", "stub-data/referencedata.get-parent-bundle-sections-defendant-level.json")
                .stubGetReferenceDataBySectionCode("IDPC",CASE_LEVEL)
                .stubGetReferenceDataBySectionCode("SJPN",DEFENDANT_LEVEL);
    }


    @Test
    public void shouldSubmitCPSMaterialOnceForCaseLevelDocuments() {

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(),
                ResourcesUtils.readResource("cps/CP20_caseLevel.xml"), headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(response.readEntity(String.class)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUploadForCpsForCaseLevel(materialUploadUrl, "12", 1);
    }

    @Test
    public void shouldSubmitCPSMaterialMultipleTimesForDefendantLevelDocuments() {

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(),
                ResourcesUtils.readResource("cps/CP20_defendantLevel.xml"), headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(response.readEntity(String.class)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUploadForCpsForDefendantLevel(materialUploadUrl, 2);
    }


    @Test
    public void shouldSubmitPETForm() {

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final String petFileName = "<FileName>Magistrates' Court PET FORM/1.pdf</FileName>";
        final String materialType= "<MaterialType>0</MaterialType>";

         final String examplefileAsString  = ResourcesUtils.readResource("cps/CP20_caseLevel.xml");
         final String payload = examplefileAsString
                 .replaceFirst("<FileName>(.*?)<\\/FileName>",petFileName)
                 .replaceFirst("<MaterialType>(.*?)<\\/MaterialType>",materialType);
         final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(),payload, headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(response.readEntity(String.class)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUploadForCaseForm(materialUploadUrl, "0", "Case Management");
    }

    @Test
    public void shouldSubmitPTPHForm() {

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final String ptphFileName = "<FileName>Magistrates' Court PTPH FORM/1.pdf</FileName>";
        final String materialType = "<MaterialType>0</MaterialType>";

        final String examplefileAsString = ResourcesUtils.readResource("cps/CP20_caseLevel.xml");
        final String payload = examplefileAsString
                .replaceFirst("<FileName>(.*?)<\\/FileName>", ptphFileName)
                .replaceFirst("<MaterialType>(.*?)<\\/MaterialType>", materialType);
        final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(), payload, headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(response.readEntity(String.class)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUploadForCaseForm(materialUploadUrl, "0", "Case Management");
    }

    @Test
    public void shouldReturnBadRequestWhenInvalidXmlReceived() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(),
                ResourcesUtils.readResource("cps/CP20_schema_invalid.xml"), headers);

        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(String.class), allOf(containsString(ERROR_MESSAGE_PREFIX), containsString(INVALID_FIELD_NAME)));
    }

    @Test
    public void shouldReturnBadRequestWhenUnSupportedFileFormat() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, UUID.randomUUID());

        final Response response = new RestClient().postCommand(CPS_DOCUMENT_UPLOAD_COMMAND_URL, ContentType.APPLICATION_XML.getMimeType(),
                ResourcesUtils.readResource("cps/CP20_caseLevel.xml").replace(".pdf",".unsupported"), headers);

        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
        assertThat(response.readEntity(String.class), containsString("File format is not supported!"));

    }
}
