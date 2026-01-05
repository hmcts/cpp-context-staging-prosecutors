package uk.gov.moj.cpp.staging.prosecutors.unbundling.service;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service.ReferenceDataService.CPS_BUNDLE_CODE;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service.ReferenceDataService.REFERENCE_DATA_GET_DOCUMENT_BUNDLE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.exception.BundleSectionsNotFoundException;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.BundleSection;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo.PDFBundleDetails;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutors.unbundling.utility.FileUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    private static final int MATERIAL_TYPE = 1;
    private static final String STUB_REFERENCEDATA_PARENT_BUNDLE_SECTIONS = "stub-data/referencedata-parent-bundle-sections.json";

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestJsonEnvelope;

    @BeforeEach
    public void setup() {
        setField(referenceDataService, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
    }

    @Test
    public void shouldReturnPDFBundleDetailsWhenValidMaterialTypePassed() throws BundleSectionsNotFoundException, IOException {
        JsonObject responsePayload = getResponsePayload();
        PDFBundleDetails convertedResponsePayload = jsonObjectToObjectConverter.convert(responsePayload, PDFBundleDetails.class);
        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);

        PDFBundleDetails response = referenceDataService.getPDFBundleDetails(MATERIAL_TYPE);

        assertNotNull(response);

        assertPDFBundleDetails(response, convertedResponsePayload);
        assertBundleSections(response.getBundleSections(), convertedResponsePayload.getBundleSections());
        assertThat(response.getAdditionalProperties(), is(convertedResponsePayload.getAdditionalProperties()));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString(CPS_BUNDLE_CODE), is(String.valueOf(MATERIAL_TYPE)));
        assertNotNull(requestJsonEnvelope.getValue().metadata().id());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCE_DATA_GET_DOCUMENT_BUNDLE));
    }

    @Test
    public void shouldThrowNotFoundBundleSectionsExceptionWhenNullResponse() throws BundleSectionsNotFoundException {
        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(null);

        assertThrows(BundleSectionsNotFoundException.class, () -> referenceDataService.getPDFBundleDetails(MATERIAL_TYPE));
    }

    private JsonObject getResponsePayload() {
        return createReader(new StringReader(FileUtil.getPayload(STUB_REFERENCEDATA_PARENT_BUNDLE_SECTIONS))).readObject();
    }

    private void assertPDFBundleDetails(PDFBundleDetails responsePojo, PDFBundleDetails expectedPojo) {
        boolean equals = Objects.equals(responsePojo.getId(), expectedPojo.getId()) &&
                Objects.equals(responsePojo.getSeqNum(), expectedPojo.getSeqNum()) &&
                Objects.equals(responsePojo.getCpsBundleCode(), expectedPojo.getCpsBundleCode()) &&
                Objects.equals(responsePojo.getParentBundleCode(), expectedPojo.getParentBundleCode()) &&
                Objects.equals(responsePojo.getParentBundleDescription(), expectedPojo.getParentBundleDescription()) &&
                Objects.equals(responsePojo.getTargetSectionCode(), expectedPojo.getTargetSectionCode()) &&
                Objects.equals(responsePojo.getUnbundleFlag(), expectedPojo.getUnbundleFlag()) &&
                Objects.equals(responsePojo.getBundleAcceptanceFlag(), expectedPojo.getBundleAcceptanceFlag()) &&
                Objects.equals(responsePojo.getValidFrom(), expectedPojo.getValidFrom()) &&
                Objects.equals(responsePojo.getValidTo(), expectedPojo.getValidTo());
        assertThat(equals, is(true));
    }

    private void assertBundleSections(List<BundleSection> responsePojoList, List<BundleSection> expectedPojoList) {
        boolean result = nonNull(responsePojoList) && nonNull(expectedPojoList) &&
                responsePojoList.size() == expectedPojoList.size();
        assertThat(result, is(true));

        List<Integer> actualList = responsePojoList.stream().map(BundleSection::getSeqNum).collect(Collectors.toList());
        List<Integer> expectedList = expectedPojoList.stream().map(BundleSection::getSeqNum).collect(Collectors.toList());

        assertThat(actualList.containsAll(expectedList), is(true));
    }
}