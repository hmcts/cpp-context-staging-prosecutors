package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileUtil.getDocumentBytesFromFile;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.moj.cpp.staging.prosecutorapi.model.event.TestDocumentBundleArrivedForUnbundling;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;
import uk.gov.moj.cpp.staging.prosecutorapi.utils.fileservice.FileServiceClient;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CmsDocumentIdentifier;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class UnbundleResultIT {

    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String DOCUMENT_FAILED_TO_UNBUNDLE_EVENT = "stagingprosecutors.event.document-failed-to-unbundle";
    private static final String DOCUMENT_UNBUNDLED_EVENT = "stagingprosecutors.event.document-unbundled-v2";
    public final String CONTEXT_NAME = "stagingprosecutors";

    private static final String PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile/cases/%s/material";
    public static final int TIMEOUT = 5000;

    private final UUID caseId = UUID.randomUUID();
    private static final String PROSECUTING_AUTHORITY = "TVL";

    private CmsDocumentIdentifier cmsDocumentIdentifier = CmsDocumentIdentifier.cmsDocumentIdentifier().withMaterialType(1).withDocumentId("ABCDEFG").build();
    private Material material = Material.material().withFileStoreId(UUID.randomUUID()).withDocumentType("documentType").withIsUnbundledDocument(true).build();
    private String prosecutorDefendantId = UUID.randomUUID().toString();
    private String defendantName = "John Rambo";
    private final WiremockUtils wiremockUtils = new WiremockUtils();
    private String materialUploadUrl = null;

    @BeforeEach
    public void setUp() {
        materialUploadUrl = format(PROSECUTION_CASE_FILE_UPLOAD_MATERIAL_COMMAND_URL, caseId);
        material = new Material(PDF_MIME_TYPE, UUID.randomUUID(), randomAlphanumeric(10), true);
        wiremockUtils
                .resetStubs()
                .stubPost(materialUploadUrl)
                .stubIdMapperRecordingNewAssociation()
                .stubReferenceDataParentBundleSections("1", "stub-data/referencedata.get-parent-bundle-sections-case-level.json");
    }

    @Test
    public void shouldRecordDocumentFailedToUnbundleEventWhenFailToUnbundleDocument() {

        final TestDocumentBundleArrivedForUnbundling event = TestDocumentBundleArrivedForUnbundling.builder()
                .caseId(caseId)
                .prosecutorDefendantId(prosecutorDefendantId)
                .defendantName(defendantName)
                .prosecutingAuthority(Optional.of(PROSECUTING_AUTHORITY))
                .material(material)
                .cmsDocumentIdentifier(cmsDocumentIdentifier)
                .receivedDateTime("2020-02-04T05:27:17.210Z")
                .build();
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DOCUMENT_FAILED_TO_UNBUNDLE_EVENT).getMessageConsumerClient();
        event.emitter()
                .addCustomMetadata("caseId", caseId.toString())
                .emit();

        Optional<String> message = messageConsumerClient.retrieveMessage(5000);
        assertThat(message.isPresent(), is(true));
    }

    @Test
    public void shouldRecordDocumentFailedToUnBundleEventWhenFailToUnBundleInvalidDocument() throws SQLException, FileServiceException {
        uploadInvalidParentBundle();

        final TestDocumentBundleArrivedForUnbundling event = TestDocumentBundleArrivedForUnbundling.builder()
                .caseId(caseId)
                .prosecutorDefendantId(prosecutorDefendantId)
                .defendantName(defendantName)
                .prosecutingAuthority(Optional.of(PROSECUTING_AUTHORITY))
                .material(material)
                .cmsDocumentIdentifier(cmsDocumentIdentifier)
                .receivedDateTime("2020-02-04T05:27:17.210Z")
                .build();
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DOCUMENT_FAILED_TO_UNBUNDLE_EVENT).getMessageConsumerClient();
        event.emitter()
                .addCustomMetadata("caseId", caseId.toString())
                .emit();

        Optional<String> message = messageConsumerClient.retrieveMessage(5000);
        assertThat(message.isPresent(), is(true));
    }

    @Test
    public void shouldRecordDocumentUnbundledEventWhenUnbundleSuccessful() throws SQLException, FileServiceException, IOException {
        uploadParentBundle();

        final TestDocumentBundleArrivedForUnbundling event = getDocumentBundleArrivedForUnbundlingPayload();

        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DOCUMENT_UNBUNDLED_EVENT).getMessageConsumerClient();
        event.emitter()
                .addCustomMetadata("caseId", caseId.toString())
                .emit();

        ObjectMapper objectMapper = new ObjectMapper();
        Optional<String> message = messageConsumerClient.retrieveMessage(TIMEOUT);
        assertThat(message.isPresent(), is(true));
        JsonNode jsonNode = objectMapper.readTree(message.get());
        assertEquals(jsonNode.get("caseId").textValue(), event.caseId.toString());
        assertEquals(jsonNode.get("receivedDateTime").textValue(), event.receivedDateTime);
        assertEquals(jsonNode.get("prosecutorDefendantId").textValue(), event.prosecutorDefendantId);
        assertEquals(jsonNode.get("prosecutingAuthority").textValue(), event.prosecutingAuthority.get());
        final Iterator<JsonNode> materials = jsonNode.get("materials").elements();
        while (materials.hasNext()) {
            final JsonNode material = materials.next();
            assertTrue(material.get("isUnbundledDocument").booleanValue());
        }
    }

    @Test
    public void shouldSendAddMaterialRequestsToProsecutionCasefileWhenDocumentUnbundleSuccessful() throws SQLException, FileServiceException, InterruptedException {
        uploadParentBundle();
        final TestDocumentBundleArrivedForUnbundling event = getDocumentBundleArrivedForUnbundlingPayload();

        event.emitter()
                .addCustomMetadata("caseId", caseId.toString())
                .emit();

        wiremockUtils.verifyRequestCount(materialUploadUrl, 1, ofSeconds(10));
        wiremockUtils.verifyUntilMaterialUploaded(materialUploadUrl, "Charges", ofSeconds(5));
        wiremockUtils.verifyUntilMaterialUploaded(materialUploadUrl, "Case Summary", ofSeconds(5));
        wiremockUtils.verifyUntilMaterialUploaded(materialUploadUrl, "Pre Cons", ofSeconds(5));
    }

    @Test
    public void shouldSendAddMaterialRequestsToProsecutionCasefileWhenMultipleDocumentUnbundleSuccessful() throws SQLException, FileServiceException, InterruptedException {
        uploadParentBundleWithMultipleDocuments();
        final TestDocumentBundleArrivedForUnbundling event = getDocumentBundleArrivedForUnbundlingPayload();

        event.emitter()
                .addCustomMetadata("caseId", caseId.toString())
                .emit();

        wiremockUtils.verifyRequestCount(materialUploadUrl, 4, ofSeconds(30));
        wiremockUtils.verifyUntilMaterialUploaded(materialUploadUrl, "Witness Statements", ofSeconds(5), 4);
    }

    private TestDocumentBundleArrivedForUnbundling getDocumentBundleArrivedForUnbundlingPayload() {
        return TestDocumentBundleArrivedForUnbundling.builder()
                .caseId(caseId)
                .prosecutorDefendantId(prosecutorDefendantId)
                .defendantName(defendantName)
                .prosecutingAuthority(Optional.of(PROSECUTING_AUTHORITY))
                .cmsDocumentIdentifier(cmsDocumentIdentifier)
                .material(material)
                .receivedDateTime("2020-02-04T05:27:17.210Z")
                .build();
    }

    private void uploadParentBundle() throws SQLException, FileServiceException {
        final byte[] documentContent = getDocumentBytesFromFile("upload_samples/IDPC_Clark_Kent.pdf");
        final UUID fileStoreId = FileServiceClient.create("IDPC_Clark_Kent.pdf", "application/pdf", documentContent);

        material = Material.material().withFileStoreId(fileStoreId)
                .withDocumentType("IDPC")
                .withIsUnbundledDocument(Boolean.TRUE)
                .build();
    }

    private void uploadParentBundleWithMultipleDocuments() throws SQLException, FileServiceException {
        final byte[] documentContent = getDocumentBytesFromFile("upload_samples/IDPC_Clark_Kent_multiple.pdf");
        final UUID fileStoreId = FileServiceClient.create("IDPC_Clark_Kent_multiple.pdf", "application/pdf", documentContent);

        material = Material.material().withFileStoreId(fileStoreId)
                .withDocumentType("IDPC")
                .withIsUnbundledDocument(Boolean.TRUE)
                .build();
    }

    private void uploadInvalidParentBundle() throws SQLException, FileServiceException {
        final byte[] documentContent = getDocumentBytesFromFile("upload_samples/INVALID_IDPC_Smith_John.pdf");
        final UUID fileStoreId = FileServiceClient.create("INVALID_IDPC_Smith_John.pdf", "application/pdf", documentContent);

        material = Material.material()
                .withFileStoreId(fileStoreId)
                .withDocumentType("IDPC")
                .build();
    }

}
