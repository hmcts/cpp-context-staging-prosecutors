package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilder;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.staging.prosecutors.converter.MediaTypeResolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResourceTest  {

    @Mock
    RestProcessor restProcessor;

    @Mock
    @Named("DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResourceActionMapper")
    ActionMapper actionMapper;


    @Mock
    private FileStorer fileStorer;

    @Spy
    private MediaTypeResolver mediaTypeResolver;
    @Mock
    InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    HttpHeaders headers;

    @Mock
    ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Mock
    TraceLogger traceLogger;

    @Mock
    HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Mock
    Response response;

    @Mock
    ParameterCollectionBuilder parameterCollectionBuilder;



    @InjectMocks
    DefaultCommandApiV1ProsecutionsCpsXmlMaterialsResource resource;

    @Test
    public void testPostHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterialsSuccessfully() throws IOException, FileServiceException {
        when(validParameterCollectionBuilderFactory.create()).thenReturn(parameterCollectionBuilder);
        when(fileStorer.store(any(),any())).thenReturn(randomUUID());
        when(response.getStatus()).thenReturn(HttpStatus.SC_ACCEPTED);
        when(restProcessor.process(any(),any(),any(),any(),any(),any(Collection.class))).thenReturn(response);

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource("CP20.xml").getFile()));

        assertThat(resource.postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials(xmlPayload).getStatus(), is(HttpStatus.SC_ACCEPTED));
        verify(restProcessor).process(any(),any(),any(),any(),any(),any(Collection.class));
        verify(fileStorer,atLeast(1)).store(any(),any());
        verify(validParameterCollectionBuilderFactory).create();
    }


    @Test
    public void testPostHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterialsWithMediaTypeException() throws IOException, FileServiceException {
        when(validParameterCollectionBuilderFactory.create()).thenReturn(parameterCollectionBuilder);

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource("CP20.xml").getFile())).replace(".docx",".unsupported");
        assertThrows(BadRequestException.class, () -> resource.postHmctsCjsCpsSubmitMaterialV1ProsecutionsCpsXmlMaterials(xmlPayload));
    }
}
