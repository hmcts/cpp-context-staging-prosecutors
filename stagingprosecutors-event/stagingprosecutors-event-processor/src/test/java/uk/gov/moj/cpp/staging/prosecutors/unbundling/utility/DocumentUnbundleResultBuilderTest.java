package uk.gov.moj.cpp.staging.prosecutors.unbundling.utility;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.APPLICATION_PDF;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.CASE_ID;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.ERROR_MESSAGE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.shared.UnbundlingConstants.PROSECUTOR_DEFENDANT_ID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.domain.Material;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.CmsDocumentIdentifier;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.DocumentBundleArrivedForUnbundling;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.utility.DocumentUnbundleResultBuilder;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentUnbundleResultBuilderTest {
    @Mock
    private EnvelopeHelper envelopeHelper;

    @Mock
    private Sender sender;

    @Mock
    private DocumentBundleArrivedForUnbundling unbundlingObject;

    @Mock
    private CmsDocumentIdentifier cmsDocumentIdentifier;

    @InjectMocks
    private DocumentUnbundleResultBuilder target;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    private UUID fileStoreId = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final String prosecutorDefendantId = "TVLA1234";
    private final String randomProsecutionAuthority = "RANDOM_PROSECUTION_AUTHORITY";
    private final ZonedDateTime receivedDateTime = ZonedDateTime.of(2018, 01, 01, 10, 12, 33, 0, ZoneId.of("UTC"));

    @BeforeEach
    public void setUp() throws Exception {
        when(unbundlingObject.getCaseId()).thenReturn(caseId);
        when(unbundlingObject.getReceivedDateTime()).thenReturn(receivedDateTime);
        when(unbundlingObject.getProsecutingAuthority()).thenReturn(randomProsecutionAuthority);
        when(unbundlingObject.getProsecutorDefendantId()).thenReturn(prosecutorDefendantId);
    }



    @Test
    public void shouldSendFailedResult() {
        target.getFailedResult(unbundlingObject, "Error occurred.");
        assertDocumentUnBundleEventArguments();
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(ERROR_MESSAGE), is("Error occurred."));
    }

    private void assertDocumentUnBundleEventArguments() {
        verify(envelopeHelper).withMetadataInPayload(jsonEnvelopeCaptor.capture());

        assertNotNull(jsonEnvelopeCaptor.getValue().payloadAsJsonObject());
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(CASE_ID), is(caseId.toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(PROSECUTOR_DEFENDANT_ID), is(prosecutorDefendantId));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString(PROSECUTING_AUTHORITY), is(randomProsecutionAuthority));
    }

    private Material getMaterial(Optional<String> documentType) {
        return Material.material()
                .withDocumentType(documentType.orElse(""))
                .withFileType(APPLICATION_PDF)
                .withFileStoreId(fileStoreId).build();
    }
}