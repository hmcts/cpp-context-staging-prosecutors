package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ProsecutorCaseReferenceUtil.getProsecutorCaseReference;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.ProsecutionCaseToProsecutionCaseFileConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.converter.SjpProsecutionToProsecutionCaseFileConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionSubmissionDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpProsecutionSubmissionDetails;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;


@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionReceivedProcessor {

    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION = "prosecutioncasefile.command.initiate-cc-prosecution";
    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION = "prosecutioncasefile.command.initiate-sjp-prosecution";

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private Clock clock;

    @Handles("stagingprosecutors.event.sjp-prosecution-received")
    public void onSjpProsecutionReceived(final Envelope<SjpProsecutionReceived> sjpnProsecutionReceivedEnvelope) {

        final SjpProsecutionReceived sjpProsecutionReceived = sjpnProsecutionReceivedEnvelope.payload();
        final ZonedDateTime dateReceived = sjpnProsecutionReceivedEnvelope.metadata().createdAt().orElse(clock.now());

        final SjpProsecutionSubmissionDetails submissionDetails = sjpProsecutionReceived.getProsecutionSubmissionDetails();
        final UUID caseFileId = systemIdMapperService.getCppCaseIdFor(
                getProsecutorCaseReference(
                        submissionDetails.getProsecutingAuthority(),
                        submissionDetails.getUrn()));
        final Converter<SjpProsecutionReceived, InitiateProsecution> prosecutionToProsecutionCaseFileConverter = new SjpProsecutionToProsecutionCaseFileConverter(caseFileId, dateReceived);

        final Metadata metadata = metadataFrom(sjpnProsecutionReceivedEnvelope.metadata())
                .withName(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION)
                .build();

        final Envelope<InitiateProsecution> envelope = envelopeFrom(metadata, prosecutionToProsecutionCaseFileConverter.convert(sjpProsecutionReceived));

        final JsonObject jsonObjectPayload = objectToJsonObjectConverter.convert(envelope.payload());
        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(metadata, jsonObjectPayload));

        sender.sendAsAdmin(jsonEnvelope);
    }

    @Handles("stagingprosecutors.event.prosecution-received")
    public void onProsecutionReceived(final Envelope<ProsecutionReceived> prosecutionReceivedEnvelope) {

        final ProsecutionReceived prosecutionReceived = prosecutionReceivedEnvelope.payload();
        final ZonedDateTime dateReceived = prosecutionReceivedEnvelope.metadata().createdAt().orElse(clock.now());
        final ProsecutionSubmissionDetails prosecutionSubmissionDetails = prosecutionReceived.getProsecutionSubmissionDetails();
        final UUID caseFileId = systemIdMapperService.getCppCaseIdFor(
                getProsecutorCaseReference(
                        prosecutionSubmissionDetails.getProsecutingAuthority(),
                        prosecutionSubmissionDetails.getUrn()));

        final Converter<ProsecutionReceived, InitiateProsecution> prosecutionToProsecutionCaseFileConverter = new ProsecutionCaseToProsecutionCaseFileConverter(caseFileId, dateReceived);

        final Metadata metadata = metadataFrom(prosecutionReceivedEnvelope.metadata())
                .withName(PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION)
                .build();

        final Envelope<InitiateProsecution> envelope = envelopeFrom(metadata, prosecutionToProsecutionCaseFileConverter.convert(prosecutionReceived));

        final JsonObject payload = objectToJsonObjectConverter.convert(envelope.payload());
        final JsonEnvelope jsonEnvelope = envelopeHelper.withMetadataInPayload(envelopeFrom(metadata, payload));

        sender.sendAsAdmin(jsonEnvelope);
    }
}
