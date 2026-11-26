package uk.gov.justice.api.resource;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilder;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.staging.prosecutors.converter.MediaTypeResolver;
import uk.gov.moj.cpp.staging.prosecutors.cps.schema.Cp20ObjectUnMarshaller;
import uk.gov.moj.cpp.staging.prosecutors.error.XmlProcessingException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.cgi.cp.cp20._2020_03.CP20;
import com.cgi.cp.cp20._2020_03.TDocument;
import org.slf4j.Logger;

@Adapter(Component.COMMAND_API)
public class DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResource implements CommandApiV1ProsecutionsCpsXmlMaterialsResource {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResource.class);

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResourceActionMapper")
    ActionMapper actionMapper;


    @Inject
    @ServiceComponent(COMMAND_API)
    private FileStorer fileStorer;

    @Inject

    private MediaTypeResolver mediaTypeResolver;
    @Inject
    InterceptorChainProcessor interceptorChainProcessor;

    @Context
    HttpHeaders headers;

    @Inject
    ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Inject
    TraceLogger traceLogger;

    @Inject
    HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Override
    public Response postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials(String payload) {
        final ParameterCollectionBuilder validParameterCollectionBuilder = validParameterCollectionBuilderFactory.create();
        final CP20 cp20 = new Cp20ObjectUnMarshaller().getCP20Type(payload);
        final List<TDocument> tDocumentList = cp20.getDocuments().getDocument();
        final String fileStoreIds = storeDocumentsInFileStore(tDocumentList);
        final String payloadWithoutFileContent  = new Cp20ObjectUnMarshaller().getMarshalContent(cp20);
        final JsonObject modifiedJsonPayload = createObjectBuilder().add("payload", payloadWithoutFileContent).add("fileStoreIds",fileStoreIds).build();

        traceLogger.trace(LOGGER, () -> String.format("Received REST request with headers: %s", httpTraceLoggerHelper.toHttpHeaderTrace(headers)));
        return restProcessor.process("AcceptedStatusEnvelopeEntityResponseStrategy", interceptorChainProcessor::process, actionMapper.actionOf("postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials", "POST", headers), Optional.of(modifiedJsonPayload), headers, validParameterCollectionBuilder.parameters());
    }

    public String storeDocumentsInFileStore(final List<TDocument> tDocumentList) {
        String fileStoreIdList = "";
        if (isNotEmpty(tDocumentList)) {
            fileStoreIdList = tDocumentList.stream().map(this::uploadSingleDocument).collect(Collectors.joining(","));
        }
        return fileStoreIdList;
    }

    private String uploadSingleDocument(final TDocument tDocument) {
        final JsonObject metadata = createObjectBuilder()
                .add("fileName", tDocument.getFileName())
                .add("createdAt", ZonedDateTimes.toString(new UtcClock().now()))
                .add("mediaType", mediaTypeResolver.resolveMediaType(tDocument.getFileName()))
                .build();

        try {
            return fileStorer.store(metadata, new ByteArrayInputStream(tDocument.getDocumentContent())).toString();
        } catch (FileServiceException e) {
            LOGGER.error("Unable to upload document in filestore", e);
            throw new XmlProcessingException("Unable to process request due to system error. Please try after some time or contact common platform helpdesk.");
        }
    }
}
