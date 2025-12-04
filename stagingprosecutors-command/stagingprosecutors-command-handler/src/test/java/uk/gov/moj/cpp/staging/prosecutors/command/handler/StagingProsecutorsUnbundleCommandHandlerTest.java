package uk.gov.moj.cpp.staging.prosecutors.command.handler;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundled;
import uk.gov.moj.cpp.staging.prosecutors.domain.DocumentUnbundledV2;
import uk.gov.moj.cpp.staging.prosecutors.domain.RecordDocumentUnbundleResult;
import uk.gov.moj.cpp.staging.prosecutors.domain.UnbundleSubmission;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DocumentFailedToUnbundle;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.RecordUnbundleDocumentResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsUnbundleCommandHandlerTest {

    private static final UUID CASE_ID = fromString("7e2f843e-d639-40b3-8611-8015f3a18958");
    private static final String STAGINGPROSECUTORS_EVENT_DOCUMENT_UNBUNDLED = "stagingprosecutors.event.document-unbundled";
    private static final String STAGINGPROSECUTORS_EVENT_DOCUMENT_UNBUNDLED_V2 = "stagingprosecutors.event.document-unbundled-v2";
    private static final String STAGINGPROSECUTORS_EVENT_DOCUMENT_FAILED_TO_UNBUNDLE = "stagingprosecutors.event.document-failed-to-unbundle";
    private static final String STAGINGPROSECUTORS_COMMAND_RECORD_DOCUMENT_UNBUNDLE_RESULT = "stagingprosecutors.command.record-document-unbundle-result";
    private static final  String STAGINGPROSECUTORS_COMMAND_RECORD_UNBUNDLE_DOCUUMENT_RESULTS ="stagingprosecutors.command.record-unbundled-document-results";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();


    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);


    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DocumentUnbundled.class,
            DocumentUnbundledV2.class,
            DocumentFailedToUnbundle.class,
            UnbundleSubmission.class);

    @InjectMocks
    private StagingProsecutorsUnbundleCommandHandler stagingProsecutorsUnbundleCommandHandler;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    private UnbundleSubmission aggregate;

    @BeforeEach
    public void setup() {
        aggregate = new UnbundleSubmission();
    }

    @Test
    public void shouldHandleUnbundleResult() {
        assertThat(new StagingProsecutorsUnbundleCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUnbundleResult")
                        .thatHandles("stagingprosecutors.command.record-document-unbundle-result")
                ));
    }

    @Test
    public void shouldHandleUnbundleDocumentResults() {
        assertThat(new StagingProsecutorsUnbundleCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUnbundleDocumentResults")
                        .thatHandles("stagingprosecutors.command.record-unbundled-document-results")
                ));
    }

    @Test
    public void shouldHandleUnbundleResultWithFailure() throws IOException, EventStreamException {

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, UnbundleSubmission.class)).thenReturn(aggregate);
        final RecordDocumentUnbundleResult recordDocumentUnbundleResult =
                handlerTestHelper.convertFromFile("json/record_unbundle_failure.json", RecordDocumentUnbundleResult.class);
        final Envelope<RecordDocumentUnbundleResult> envelope =
                envelopeFrom(metadataFor(STAGINGPROSECUTORS_COMMAND_RECORD_DOCUMENT_UNBUNDLE_RESULT, CASE_ID), recordDocumentUnbundleResult);

        stagingProsecutorsUnbundleCommandHandler.handleUnbundleResult(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                STAGINGPROSECUTORS_EVENT_DOCUMENT_FAILED_TO_UNBUNDLE,
                handlerTestHelper.convertFromFile("json/record_unbundle_failure.json", JsonValue.class));
    }

    @Test
    public void shouldHandleUnbundleResultWithSuccess() throws IOException, EventStreamException {

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, UnbundleSubmission.class)).thenReturn(aggregate);
        final RecordDocumentUnbundleResult recordDocumentUnbundleResult =
                handlerTestHelper.convertFromFile("json/record_unbundle_success.json", RecordDocumentUnbundleResult.class);
        final Envelope<RecordDocumentUnbundleResult> envelope =
                envelopeFrom(metadataFor(STAGINGPROSECUTORS_COMMAND_RECORD_DOCUMENT_UNBUNDLE_RESULT, CASE_ID), recordDocumentUnbundleResult);

        stagingProsecutorsUnbundleCommandHandler.handleUnbundleResult(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                STAGINGPROSECUTORS_EVENT_DOCUMENT_UNBUNDLED,
                handlerTestHelper.convertFromFile("json/record_unbundle_success_response.json", JsonValue.class));
    }

    @Test
    public void shouldHandleUnbundleDocuumentResultsWithFailure() throws IOException, EventStreamException {

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, UnbundleSubmission.class)).thenReturn(aggregate);
        final RecordUnbundleDocumentResults recordUnbundleDocumentResults =
                handlerTestHelper.convertFromFile("json/record_unbundle_failure.json", RecordUnbundleDocumentResults.class);
        final Envelope<RecordUnbundleDocumentResults> envelope =
                envelopeFrom(metadataFor(STAGINGPROSECUTORS_COMMAND_RECORD_UNBUNDLE_DOCUUMENT_RESULTS, CASE_ID), recordUnbundleDocumentResults);

        stagingProsecutorsUnbundleCommandHandler.handleUnbundleDocumentResults(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                STAGINGPROSECUTORS_EVENT_DOCUMENT_FAILED_TO_UNBUNDLE,
                handlerTestHelper.convertFromFile("json/record_unbundle_failure.json", JsonValue.class));
    }

    @Test
    public void shouldHandleUnbundleResultWithSuccessWhenMultipleMaterials() throws IOException, EventStreamException {

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, UnbundleSubmission.class)).thenReturn(aggregate);
        final RecordUnbundleDocumentResults recordDocumentUnbundleResult =
                handlerTestHelper.convertFromFile("json/record_unbundle_success_multi-material.json", RecordUnbundleDocumentResults.class);
        final Envelope<RecordUnbundleDocumentResults> envelope =
                envelopeFrom(metadataFor(STAGINGPROSECUTORS_COMMAND_RECORD_DOCUMENT_UNBUNDLE_RESULT, CASE_ID), recordDocumentUnbundleResult);

        stagingProsecutorsUnbundleCommandHandler.handleUnbundleDocumentResults(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                STAGINGPROSECUTORS_EVENT_DOCUMENT_UNBUNDLED_V2,
                handlerTestHelper.convertFromFile("json/record_unbundle_success_response_multi_materials.json", JsonValue.class)
        );
    }
}