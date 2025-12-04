package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.extractSubmissionId;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.getSubmissionStatusForCPS;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmission;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmissionSjpV3;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmissionV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.pollForSubmissionV3;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicMaterialSubmissionPendingWithWarning;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicMaterialSubmissionRejected;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicMaterialSubmissionRejectedV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicMaterialSubmissionRejectedWithWarning;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.publishPublicProgressionCourtDocumentAdded;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendCpsFileUploadRequestV1;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendFileUploadRequest;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendFileUploadRequestV2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.sendFileUploadRequestV3;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.verifyForbiddenHttpResponseCode;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.verifyForbiddenHttpResponseCodeForV3Submission;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING_WITH_WARNINGS;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.REJECTED;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.SUCCESS;

import uk.gov.moj.cpp.staging.prosecutorapi.model.common.Problem;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SubmissionType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubmitMaterialIT {

    private static final String PROSECUTING_AUTHORITY = "TVL";
    private static final String MATERIAL_TYPE = "SJPN";
    private static final String PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cases/%s/material";
    private static final String PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_FOR_COURT_APPLICATION_COMMAND_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/applications/%s/material";
    private UUID CASE_ID = null;
    private final WiremockUtils wiremockUtils = new WiremockUtils();
    private String materialUploadUrl = null;
    private static final String REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS = "stub-data/referencedata.get-prosecutor-cps.json";
    private static final String REF_DATA_PROSECUTOR_STUB_RESOURCE_NON_CPS = "stub-data/referencedata.get-prosecutor-non-cps.json";

    @BeforeEach
    public void stub() {
        CASE_ID = randomUUID();
        materialUploadUrl = format(PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL, CASE_ID);

        wiremockUtils
                .stubPost(materialUploadUrl)
                .stubIdMapperReturningExistingAssociation(CASE_ID);
    }

    @Test
    public void shouldReturnBadRequestIfRequiredFieldIsNotProvided() throws Exception {
        final String caseUrn = STRING.next();
        final String invalidMaterialType = null;

        final HttpResponse response = sendFileUploadRequest(caseUrn, getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"), invalidMaterialType, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void shouldReturnBadRequestIfProvidedCaseUrnExceedsMaxLength() throws Exception {
        final String tooLongCaseUrn = "this_case_urn_is_too_long_so_that_it_fails_validation";

        final HttpResponse response = sendFileUploadRequest(tooLongCaseUrn, getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"), MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void shouldSubmitMaterial() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequest(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, SubmissionStatus.PENDING);

        wiremockUtils.verifyMaterialUpload(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);
    }

    @Test
    public void shouldSubmitMaterialV2() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequestV2(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmissionV2(submissionId, PENDING, PROSECUTING_AUTHORITY);

        wiremockUtils.verifyMaterialUpload(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);
    }

    @Test
    public void shouldSubmitMaterialCpsV1() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendCpsFileUploadRequestV1(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        final SubmissionStatus status = getSubmissionStatusForCPS(submissionId);
        assertThat(status.toString(), is(PENDING.toString()));

        wiremockUtils.verifyMaterialUpload(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);
    }

    @Test
    public void shouldFailQueryNonCpsFileWithGetSubmissionStatusForCPS() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequestV2(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        verifyForbiddenHttpResponseCode(submissionId);
    }

    @Test
    public void shouldSubmitMaterialV2AndSetStatusToSuccess() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequestV2(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                MATERIAL_TYPE, PROSECUTING_AUTHORITY, PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, PENDING);

        wiremockUtils.verifyMaterialUpload(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);

        publishPublicProgressionCourtDocumentAdded(submissionId);

        pollForSubmissionV2(submissionId, SUCCESS, PROSECUTING_AUTHORITY);
    }

    @Test
    public void shouldHandleRejectedMaterialSubmission() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequest(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                STRING.next(), PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, PENDING);

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejected(CASE_ID, submissionId, problem);

        pollForSubmission(submissionId,
                withJsonPath("status", is(REJECTED.toString())),
                withJsonPath("completedAt", notNullValue()),
                withJsonPath("errors[0].code", is(problem.code)),
                withJsonPath("errors[0].values[0].key", is(problem.values.get(0).key)),
                withJsonPath("errors[0].values[0].value", is(problem.values.get(0).value)),
                withJsonPath("type", is(SubmissionType.MATERIAL.toString())));

    }

    @Test
    public void shouldHandleRejectedMaterialSubmissionV2() throws Exception {
        final String caseUrn = STRING.next();

        final HttpResponse response = sendFileUploadRequest(caseUrn,
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                STRING.next(), PROSECUTING_AUTHORITY);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, PENDING);

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejected(CASE_ID, submissionId, problem);

        pollForSubmissionV2(submissionId, PROSECUTING_AUTHORITY,
                withJsonPath("status", is(REJECTED.toString())),
                withJsonPath("completedAt", notNullValue()),
                withJsonPath("errors[0].code", is(problem.code)),
                withJsonPath("errors[0].values[0].key", is(problem.values.get(0).key)),
                withJsonPath("errors[0].values[0].value", is(problem.values.get(0).value)),
                withJsonPath("type", is(SubmissionType.MATERIAL.toString())));

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v2.Submission submission = pollForSubmissionV2(submissionId, REJECTED, PROSECUTING_AUTHORITY);

        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getErrors(), containsInAnyOrder(problem));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));
    }

    @Test
    public void shouldSubmitMaterialV3ForCaseAndSetStatusToPending() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final HttpResponse response = sendFileUploadRequestV3(
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                PROSECUTING_AUTHORITY, true, null, OUCODE);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission submission = pollForSubmissionV3(submissionId, PENDING, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));

        wiremockUtils.verifyMaterialUploadForCaseV2(materialUploadUrl, MATERIAL_TYPE, PROSECUTING_AUTHORITY);
    }

    @Test
    public void shouldSubmitMaterialV3ForCourApplicationAndSetStatusToPending() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final UUID applicationId = randomUUID();

        final HttpResponse response = sendFileUploadRequestV3(
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                PROSECUTING_AUTHORITY, false, applicationId, OUCODE);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));

        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission submission = pollForSubmissionV3(submissionId, PENDING, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());


        materialUploadUrl = format(PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_FOR_COURT_APPLICATION_COMMAND_URL, applicationId);
        wiremockUtils
                .stubPost(materialUploadUrl)
                .stubIdMapperReturningExistingAssociation(applicationId);

        wiremockUtils.verifyMaterialUploadForCourtApplication(materialUploadUrl, MATERIAL_TYPE, applicationId);

    }

    @Test
    public void shouldHandleRejectedMaterialSubmissionV3() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final UUID submissionId = submitMaterial();

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejectedV2(CASE_ID, submissionId, problem);

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission submission = pollForSubmissionV3(submissionId, REJECTED, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());
        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getErrors(), containsInAnyOrder(problem));
        assertThat(submission.getWarnings().size(), is(0));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));
    }

    @Test
    public void shouldHandleRejectedMaterialSubmissionV3WithWarnings() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final UUID submissionId = submitMaterial();

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejectedWithWarning(CASE_ID, submissionId, problem);

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission submission = pollForSubmissionV3(submissionId, REJECTED, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());
        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getErrors(), containsInAnyOrder(problem));
        assertThat(submission.getWarnings(), containsInAnyOrder(problem));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));
    }


    @Test
    public void shouldHandleMaterialSubmissionPendingWithWarningsV3() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final UUID submissionId = submitMaterial();

        final Problem problem = new Problem("DEFENDANT_ON_CP", List.of(new Problem.ProblemValue("defendant", "NOT IN CP")));

        publishPublicMaterialSubmissionPendingWithWarning(CASE_ID, submissionId, problem);

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.Submission submission = pollForSubmissionV3(submissionId, PENDING_WITH_WARNINGS, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());
        assertThat(submission.getWarnings(), containsInAnyOrder(problem));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));

    }

    @Test
    public void shouldTestGetSubmissionSJPV3() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode(PROSECUTING_AUTHORITY, REF_DATA_PROSECUTOR_STUB_RESOURCE_CPS);

        final UUID submissionId = submitMaterial();

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejectedWithWarning(CASE_ID, submissionId, problem);

        final uk.gov.moj.cpp.staging.prosecutorapi.model.query.v3.SjpSubmission submission = pollForSubmissionSjpV3(submissionId, REJECTED, PROSECUTING_AUTHORITY);
        assertThat(submission.getReceivedAt(), notNullValue());
        assertThat(submission.getCompletedAt(), notNullValue());
        assertThat(submission.getErrors(), containsInAnyOrder(problem));
        assertThat(submission.getWarnings(), containsInAnyOrder(problem));
        assertThat(submission.getType(), is(SubmissionType.MATERIAL.toString()));
    }

    @Test
    public void shouldThrowForbiddenIfNonCpsOuCodeInHeaderAndUnmatchedProsecutingAuthority() throws Exception {

        wiremockUtils.stubGetProsecutorByOuCode("TFL", REF_DATA_PROSECUTOR_STUB_RESOURCE_NON_CPS);

        final UUID submissionId = submitMaterial();

        final Problem problem = new Problem("INVALID_DOCUMENT_TYPE", List.of(new Problem.ProblemValue("documentType", "PLEA")));

        publishPublicMaterialSubmissionRejectedWithWarning(CASE_ID, submissionId, problem);

        verifyForbiddenHttpResponseCodeForV3Submission(submissionId, "TFL");

    }

    private String extractResponse(final HttpResponse response) throws IOException {
        return IOUtils.toString(response.getEntity().getContent());
    }

    private File getFileFrom(final String filePath) {
        return new File(getResource(filePath).getFile());
    }

    private UUID submitMaterial() throws Exception {

        final UUID applicationId = randomUUID();

        final HttpResponse response = sendFileUploadRequestV3(
                getFileFrom("submitProsecutionDocument/41b2Y9DaQ3L.jpg"),
                PROSECUTING_AUTHORITY, true, applicationId, OUCODE);

        assertThat(response.getStatusLine().getStatusCode(), is(ACCEPTED.getStatusCode()));
        final UUID submissionId = extractSubmissionId(Optional.of(extractResponse(response)));

        pollForSubmission(submissionId, PENDING);

        return submissionId;
    }
}
