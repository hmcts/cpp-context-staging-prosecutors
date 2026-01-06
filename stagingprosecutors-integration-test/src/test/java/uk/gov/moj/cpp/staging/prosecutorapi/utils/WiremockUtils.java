package uk.gov.moj.cpp.staging.prosecutorapi.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.ResourcesUtils.readResource;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil.resourceToString;

import java.time.Duration;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;

public class WiremockUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int PORT = 8080;
    private static final int POLL_INTERVAL = 100;
    private static final int POLL_DELAY = 50;

    private static final String SYSTEM_ID_MAPPER_URL = "/system-id-mapper-api/rest/systemid/mappings/*";
    private static final String USER_GROUPS_USERS_QUERY_URL = "/usersgroups-service/query/api/rest/usersgroups/users/.*/groups";
    private static final String REFERENCE_DATA_ACTION_PARENT_BUNDLE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/parent-bundle-section";
    private static final String REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/documents-type-access";
    private static final String REFERENCE_DATA_ACTION_APPLICATION_TYPES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/application-types";
    private static final String REFERENCE_DATA_ACTION_PUBLIC_HOLIDAYS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/public-holidays/*";

    private static final String REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/prosecutors";
    private static final String REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE = "application/vnd.referencedata.query.get.prosecutor+json";

    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE = "application/vnd.referencedata.query.document-type-access-by-sectioncode+json";
    private static final String REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE = "application/vnd.reference-data.parent-bundle-section+json";
    private static final String REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE = "application/vnd.referencedata.application-types+json";
    private static final String REFERENCE_DATA_ACTION_PUBLIC_HOLIDAYS_MEDIA_TYPE = "application/vnd.referencedata.query.public-holidays+json";

    public WiremockUtils() {
        configureFor(HOST, PORT);
        reset();
        stubUserAndGroups();
    }

    public WiremockUtils stubPost(String url) {
        stubFor(post(
                urlMatching(url))
                .willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        return this;
    }

    public WiremockUtils stubIdMapperRecordingNewAssociation() {
        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(systemIdResponseTemplate(randomUUID()))));

        return this;
    }

    public WiremockUtils stubIdMapperReturningExistingAssociation(final UUID associationId) {
        stubFor(get(urlPathMatching(SYSTEM_ID_MAPPER_URL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(systemIdMappingResponseTemplate(associationId))));
        return this;
    }

    public WiremockUtils stubReferenceDataParentBundleSections(final String cpsBundleCode, final String responseFile) {
        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_PARENT_BUNDLE_QUERY_URL))
                .withQueryParam("cpsBundleCode", equalTo(cpsBundleCode))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE)
                        .withBody(resourceToString(responseFile))));

        return this;
    }

    public void stubReferenceDataCourtApplicationTypes() {
        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_APPLICATION_TYPES_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.application-types.json"))));

    }

    public void stubReferenceDataPublicHolidays() {
        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_PUBLIC_HOLIDAYS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PUBLIC_HOLIDAYS_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PUBLIC_HOLIDAYS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.public-holidays.json"))));

    }

    public WiremockUtils stubGetReferenceDataBySectionCode(final String sectionCode, final String documentCategory) {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL))
                .withQueryParam("sectionCode", equalTo(sectionCode))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.query.document-type-access-by-sectionCode.json").replace("DOCUMENT_CATEGORY", documentCategory))));

        return this;
    }

    public WiremockUtils stubGetProsecutorByOuCode(String ouCode, String resource) {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withQueryParam("oucode", equalTo(ouCode))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString(resource))));

        return this;
    }

    public WiremockUtils stubGetProsecutionResults(final String ouCode) {
        stubFor(get(urlPathMatching(".*query/api/rest/results/prosecutor/" + ouCode + ".*"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.results.prosecutor-results+json")
                        .withBody(readResource("stub-data/hmcts-prosecutor-results-v1.json"))));
        return this;
    }

    public WiremockUtils stubGetProsecutionResultsForException(final String ouCode) {
        stubFor(get(urlPathMatching(".*query/api/rest/results/prosecutor/" + ouCode + ".*"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.results.prosecutor-results+json")
                        .withBody("Error calling results service - Internal Server Error")));
        return this;
    }

    public void verifyMaterialUpload(final String url, final String materialType, final String prosecutionAuthority) {
        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-material+json"))
                .withRequestBody(containing(materialType))
                .withRequestBody(containing(prosecutionAuthority))
                .withRequestBody(containing("fileStoreId"))// Auto generated, so can't verify the value
        );
    }

    public void verifyMaterialUploadForCaseV2(final String url, final String materialType, final String prosecutionAuthority) {
        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-material-v2+json"))
                .withRequestBody(containing(materialType))
                .withRequestBody(containing(prosecutionAuthority))
                .withRequestBody(containing("material"))
                .withRequestBody(containing("1985-02-03"))
                .withRequestBody(containing("prosecutionCaseSubject"))
                .withRequestBody(containing("receivedDateTime"))
                .withRequestBody(containing("ouCode"))
        );
    }

    public void verifyMaterialUploadForCourtApplication(final String url, final String materialType, final UUID applicationId) {
        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-application-material-v2+json"))
                .withRequestBody(containing(materialType))
                .withRequestBody(containing("material"))
                .withRequestBody(containing("courtApplicationSubject"))
                .withRequestBody(containing(applicationId.toString()))
        );
    }

    public void verifyUntilMaterialUploaded(final String url, final String documentType, Duration timeout) {
        Awaitility.await().timeout(timeout)
                .pollInterval(POLL_INTERVAL, MILLISECONDS)
                .pollDelay(POLL_DELAY, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(url))
                                .withRequestBody(containing(documentType))).size(),
                        CoreMatchers.is(1));
    }

    public void verifyUntilMaterialUploaded(final String url, final String documentType, Duration timeout, final int count) {
        Awaitility.await().timeout(timeout)
                .pollInterval(POLL_INTERVAL, MILLISECONDS)
                .pollDelay(POLL_DELAY, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(url))
                                .withRequestBody(containing(documentType))).size(),
                        CoreMatchers.is(count));
    }

    public void verifyRequestCount(final String url, int count, Duration timeout) {
        Awaitility.await().timeout(timeout)
                .pollInterval(100, MILLISECONDS)
                .pollDelay(50, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(url))).size(),
                        CoreMatchers.is(count));
    }

    public void verifyMaterialUploadForCpsForCaseLevel(final String url, final String materialType, final int expectedServiceCallCount) {
        Awaitility.await().timeout(ofSeconds(5))
                .pollInterval(100, MILLISECONDS)
                .pollDelay(50, MILLISECONDS)
                .until(
                        () -> findAll(
                                postRequestedFor(urlPathMatching(url))
                                        .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-cps-material+json"))
                                        .withRequestBody(containing(materialType))
                                        .withRequestBody(containing("fileStoreId"))
                        ).size(),
                        CoreMatchers.is(expectedServiceCallCount));

    }

    public void verifyMaterialUploadForCpsForDefendantLevel(final String url, final int expectedServiceCallCount) {
        Awaitility.await().timeout(ofSeconds(5))
                .pollInterval(100, MILLISECONDS)
                .pollDelay(50, MILLISECONDS)
                .until(
                        () -> findAll(
                                postRequestedFor(urlPathMatching(url))
                                        .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-cps-material+json"))
                                        .withRequestBody(containing("prosecutorDefendantId"))
                                        .withRequestBody(containing("fileStoreId"))
                        ).size(),
                        CoreMatchers.is(expectedServiceCallCount));

    }

    public void verifyMaterialUploadForCaseForm(final String url, final String materialType, final String documentType) {
        verify(postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo("application/vnd.prosecutioncasefile.add-cps-material+json"))
                .withRequestBody(containing(materialType))
                .withRequestBody(containing(documentType))
                .withRequestBody(containing("fileStoreId")) // Auto generated, so can't verify the value
        );
    }

    private String systemIdResponseTemplate(final UUID associationId) {
        return "{\n" +
                "\t\"_metadata\": {\n" +
                "\t\t\"id\": \"f2426280-f4d7-45cf-9f94-c618a210f7c2\",\n" +
                "\t\t\"name\": \"systemid.map\"\n" +
                "\t},\n" +
                "\t\"id\": \"" + associationId.toString() + "\"\n" +
                "}";
    }

    private String systemIdMappingResponseTemplate(final UUID associationId) {

        return "{\n" +
                "  \"mappingId\": \"166c0ae9-e276-4d29-b669-cb32013228b3\",\n" +
                "  \"sourceId\": \"ID01\",\n" +
                "  \"sourceType\": \"SystemACaseId\",\n" +
                "  \"targetId\": \"" + associationId + "\",\n" +
                "  \"targetType\": \"caseId\",\n" +
                "  \"createdAt\": \"2016-09-07T14:30:53.294Z\"\n" +
                "}";
    }

    private void stubUserAndGroups() {
        stubFor(get(urlMatching(USER_GROUPS_USERS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(readResource("stub-data/usersgroups.get-groups-by-user.json"))));
    }

    public WiremockUtils resetStubs() {
        reset();
        resetAllRequests();
        return this;
    }


    public WiremockUtils stubAccessControl(final boolean grantAccess, final UUID userId, final String... groupNames) {

        final JsonArrayBuilder groupsArray = JsonObjects.createArrayBuilder();

        if (grantAccess) {
            for (final String groupName : groupNames) {
                groupsArray.add(createObjectBuilder()
                        .add("groupId", randomUUID().toString())
                        .add("groupName", groupName)
                );
            }
        }

        final JsonObject response = createObjectBuilder()
                .add("groups", groupsArray).build();

        stubFor(get(urlPathMatching("/usersgroups-service/query/api/rest/usersgroups/users/[^/]*/groups"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(ID, userId.toString())
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(response.toString())));
        return this;
    }
}
