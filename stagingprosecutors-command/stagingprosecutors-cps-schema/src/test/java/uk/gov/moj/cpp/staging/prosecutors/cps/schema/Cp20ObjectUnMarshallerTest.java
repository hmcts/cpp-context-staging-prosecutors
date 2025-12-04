package uk.gov.moj.cpp.staging.prosecutors.cps.schema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.io.File;
import java.io.IOException;

import com.cgi.cp.cp20._2020_03.CP20;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Cp20ObjectUnMarshallerTest {


    public static final String CP20_XML_FILE_NAME = "CP20_CGI.xml";
    public static final String CP20_XML_FILE_NAME_EXPECTED = "CP20_CGI_EXPECTED.xml";
    public static final String CP20_INVALID_MATERIAL_XML_FILE_NAME = "CP20_CGI_INVALID_MATERIAL_TYPE.xml";
    public static final String CP20_INVALID_MATERIAL_2_XML_FILE_NAME = "CP20_CGI_INVALID_MATERIAL_2_TYPE.xml";

    @InjectMocks
    private Cp20ObjectUnMarshaller cp20ObjectUnMarshaller;

    @Test
    public void shouldReturnCP20TypeForGivenXmlPayload() throws IOException {

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource(CP20_XML_FILE_NAME).getFile()));

        final CP20 cp20 = cp20ObjectUnMarshaller.getCP20Type(xmlPayload);

        assertThat(cp20.getTransactionID(), is(29990));
        assertThat(cp20.getCompassCaseId(), is(6301));
        assertThat(cp20.getURN(), is("45GD4371025"));
        assertThat(cp20.getResponseEmail(), is("SurreyCJU@logica.com"));

        assertThat(cp20.getDocuments().getDocument().size(), is(2));
        assertThat(cp20.getDocuments().getDocument().get(0).getDocumentId(), is("D372133"));
        assertThat(cp20.getDocuments().getDocument().get(1).getDocumentId(), is("D372138"));


        assertThat(cp20.getDefendants().getDefendant().size(), is(2));
        assertThat(cp20.getDefendants().getDefendant().get(0).getASN(), is("1011PP0000000500001B"));
        assertThat(cp20.getDefendants().getDefendant().get(0).getSurname(), is("TWOOWESEVEN"));
        assertThat(cp20.getDefendants().getDefendant().get(0).getForenames(), is("Jack"));
        assertThat(cp20.getDefendants().getDefendant().get(0).getDOB().toString(), is("1999-01-01"));
        assertThat(cp20.getDefendants().getDefendant().get(0).getOUCode(), is("B13CC00"));
        assertThat(cp20.getDefendants().getDefendant().get(0).getCMSDefendantID().intValue(), is(11235617));
        assertThat(cp20.getDefendants().getDefendant().get(1).getCMSDefendantID().intValue(), is(11235757));

    }


    @Test
    public void shouldReturnXmlForForGivenXmlPayload() throws IOException {

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource(CP20_XML_FILE_NAME).getFile()));

        final CP20 cp20 = cp20ObjectUnMarshaller.getCP20Type(xmlPayload);

        assertThat(cp20ObjectUnMarshaller.getMarshalContent(cp20),containsString("<DocumentId>D372133</DocumentId>"));
        assertThat(cp20ObjectUnMarshaller.getMarshalContent(cp20),containsString("<FileName>Primary - items (pre Apr05)200128_171946-247.docx</FileName>"));
        assertThat(cp20ObjectUnMarshaller.getMarshalContent(cp20),containsString("<OUCode>B13CC00</OUCode>"));
        assertThat(cp20ObjectUnMarshaller.getMarshalContent(cp20),containsString("<Surname>BINARY</Surname>"));

    }

    @Test
    public void shouldBadXmlPayloadForMaterialTypeNotValid() throws IOException {

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource(CP20_INVALID_MATERIAL_XML_FILE_NAME).getFile()));

        assertThrows(BadRequestException.class, () -> cp20ObjectUnMarshaller.getCP20Type(xmlPayload));

    }



    @Test
    public void shouldBadXmlPayloadForMaterialTypeMoreNumberNotValid() throws IOException {

        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource(CP20_INVALID_MATERIAL_2_XML_FILE_NAME).getFile()));

        assertThrows(BadRequestException.class, () -> cp20ObjectUnMarshaller.getCP20Type(xmlPayload));

    }

    @Test
    public void shouldThrowXmlProcessingExceptionWhenInvalidXml() {
        final String invalidXmlPayload = "<test></test>";
        assertThrows(BadRequestException.class, () -> cp20ObjectUnMarshaller.getCP20Type(invalidXmlPayload));
    }

}