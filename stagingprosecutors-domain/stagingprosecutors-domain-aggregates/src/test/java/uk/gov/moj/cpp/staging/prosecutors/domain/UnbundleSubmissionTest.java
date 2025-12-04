package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DocumentFailedToUnbundle;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnbundleSubmissionTest {

    @InjectMocks
    private UnbundleSubmission unbundleSubmission;

    @Test
    public void shouldRaiseDocumentFailedToUnbundleEventWhenErrorMessageIsNotNull() {

        final UUID caseId = randomUUID();
        final String prosecutorDefendantId = randomUUID().toString();
        final Optional<String> prosecutingAuthority = of("prosecutingAuthority");
        final Optional<ZonedDateTime> receivedDateTime = of(ZonedDateTime.now());
        final Optional<Material> material = of(Material.material().withFileStoreId(randomUUID()).build());
        final Optional<String> errorMessage = of("error occurred");

        final Stream<Object> eventStream = unbundleSubmission.submitUnbundleResultRecord(caseId, prosecutorDefendantId, prosecutingAuthority, receivedDateTime, material, errorMessage);

        final DocumentFailedToUnbundle documentFailedToUnbundle = (DocumentFailedToUnbundle) eventStream.findFirst().get();
        assertThat(documentFailedToUnbundle.getCaseId(), is(caseId));
        assertThat(documentFailedToUnbundle.getProsecutorDefendantId(), is(prosecutorDefendantId));
        assertThat(documentFailedToUnbundle.getProsecutingAuthority(), is(prosecutingAuthority.get()));
        assertThat(documentFailedToUnbundle.getErrorMessage(), is(errorMessage.get()));
    }

    @Test
    public void shouldRaiseDocumentUnbundledEventWhenErrorMessageIsNull() {

        final UUID caseId = randomUUID();
        final String prosecutorDefendantId = randomUUID().toString();
        final Optional<String> prosecutingAuthority = of("prosecutingAuthority");
        final Optional<ZonedDateTime> receivedDateTime = of(ZonedDateTime.now());
        final Optional<Material> material = of(Material.material().withFileStoreId(randomUUID()).build());

        final Stream<Object> eventStream = unbundleSubmission.submitUnbundleResultRecord(caseId, prosecutorDefendantId, prosecutingAuthority, receivedDateTime, material, empty());

        final DocumentUnbundled documentUnbundled = (DocumentUnbundled) eventStream.findFirst().get();
        assertThat(documentUnbundled.getCaseId(), is(caseId));
        assertThat(documentUnbundled.getProsecutorDefendantId(), is(prosecutorDefendantId));
        assertThat(documentUnbundled.getProsecutingAuthority().get(), is(prosecutingAuthority.get()));
        assertThat(documentUnbundled.getMaterial().getFileStoreId(), is(material.get().getFileStoreId()));
        assertThat(documentUnbundled.getReceivedDateTime(), is(receivedDateTime.get()));
    }

    @Test
    public void shouldRaiseDocumentFailedToUnbundleEventWhenErrorMessageIsNotNullOnSubmitUnbundleDocumentResults() {

        final UUID caseId = randomUUID();
        final String prosecutorDefendantId = randomUUID().toString();
        final String prosecutingAuthority = "prosecutingAuthority";
        final ZonedDateTime receivedDateTime = ZonedDateTime.now();
        final List<Material> materials = singletonList(Material.material().withFileStoreId(randomUUID()).build());
        final String errorMessage = "error occurred";

        final Stream<Object> eventStream = unbundleSubmission.submitUnbundleDocumentResults(caseId, prosecutorDefendantId, prosecutingAuthority, receivedDateTime, materials, errorMessage);

        final DocumentFailedToUnbundle documentFailedToUnbundle = (DocumentFailedToUnbundle) eventStream.findFirst().get();
        assertThat(documentFailedToUnbundle.getCaseId(), is(caseId));
        assertThat(documentFailedToUnbundle.getProsecutorDefendantId(), is(prosecutorDefendantId));
        assertThat(documentFailedToUnbundle.getProsecutingAuthority(), is(prosecutingAuthority));
        assertThat(documentFailedToUnbundle.getErrorMessage(), is(errorMessage));
    }

    @Test
    public void shouldRaiseDocumentUnbundledEventWhenErrorMessageIsNullOnSubmitUnbundleDocumentResults() {

        final UUID caseId = randomUUID();
        final String prosecutorDefendantId = randomUUID().toString();
        final String prosecutingAuthority = "prosecutingAuthority";
        final ZonedDateTime receivedDateTime = ZonedDateTime.now();
        final List<Material> materials = singletonList(Material.material().withFileStoreId(randomUUID()).build());

        final Stream<Object> eventStream = unbundleSubmission.submitUnbundleDocumentResults(caseId, prosecutorDefendantId, prosecutingAuthority, receivedDateTime, materials, null);

        final DocumentUnbundledV2 documentUnbundled = (DocumentUnbundledV2) eventStream.findFirst().get();
        assertThat(documentUnbundled.getCaseId(), is(caseId));
        assertThat(documentUnbundled.getProsecutorDefendantId(), is(prosecutorDefendantId));
        assertThat(documentUnbundled.getProsecutingAuthority().get(), is(prosecutingAuthority));
        assertThat(documentUnbundled.getMaterials().get(0).getFileStoreId(), is(materials.get(0).getFileStoreId()));
        assertThat(documentUnbundled.getReceivedDateTime(), is(receivedDateTime));
    }

}
