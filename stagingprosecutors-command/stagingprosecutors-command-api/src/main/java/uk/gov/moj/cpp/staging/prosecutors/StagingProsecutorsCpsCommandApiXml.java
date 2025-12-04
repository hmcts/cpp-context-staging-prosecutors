package uk.gov.moj.cpp.staging.prosecutors;

import static java.util.Collections.EMPTY_LIST;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.converter.SubmitCpsMaterialConverter;
import uk.gov.moj.cpp.staging.prosecutors.cps.schema.Cp20ObjectUnMarshaller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.cgi.cp.cp20._2020_03.CP20;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitCpsMaterialCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsCpsCommandApiXml {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingProsecutorsCpsCommandApiXml.class);

    @Inject
    private Sender sender;


    @Inject
    private SubmitCpsMaterialConverter submitCpsMaterialConverter;


    @Handles("hmcts.cjs.cps-submit-material")
    public Envelope<JsonObject> submitMaterial(final JsonEnvelope envelope) {
        final String xmlPayload = envelope.payloadAsJsonObject().getString("payload");
        final String fileStoreIdsCommaSeparated = envelope.payloadAsJsonObject().getString("fileStoreIds");
        LOGGER.info("file ids received from CPS: {}", fileStoreIdsCommaSeparated);

        final CP20 cp20 = new Cp20ObjectUnMarshaller().getCP20Type(xmlPayload);
        LOGGER.info("XML payload received from CPS with caseURN :{}", cp20.getURN());

        List<UUID> fileStoreIdsList = EMPTY_LIST;
        if (!StringUtils.isEmpty(fileStoreIdsCommaSeparated)) {
            fileStoreIdsList = Stream.of(fileStoreIdsCommaSeparated.split(",", -1)).map(UUID::fromString)
                .collect(Collectors.toList());
        }
        final SubmitCpsMaterialCommand submitCpsMaterialCommand = submitCpsMaterialConverter.convert(cp20, fileStoreIdsList);
        final JsonObject response = createObjectBuilder()
                .add("submissionId", submitCpsMaterialCommand.getSubmissionId().toString())
                .build();

        sender.send(envelop(submitCpsMaterialCommand)
                .withName("stagingprosecutors.command.submit-cps-material")
                .withMetadataFrom(envelope));

        return envelopeFrom(envelope.metadata(), response);
    }

}
