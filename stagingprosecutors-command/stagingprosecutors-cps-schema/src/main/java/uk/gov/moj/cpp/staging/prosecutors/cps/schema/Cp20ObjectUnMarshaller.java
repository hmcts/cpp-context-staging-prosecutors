package uk.gov.moj.cpp.staging.prosecutors.cps.schema;

import static javax.xml.bind.JAXBContext.newInstance;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.cgi.cp.cp20._2020_03.CP20;
import org.xml.sax.SAXException;

public class Cp20ObjectUnMarshaller {


    private static final String CP20_XSD = "xsd/cp20.xsd";
    private static final JAXBContext jaxbContext;
    static {
        try {
            // one time instance creation
            jaxbContext = newInstance(CP20.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    public CP20 getCP20Type(final String payload) {
        try {

            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = sf.newSchema(Thread.currentThread().getContextClassLoader().getResource(CP20_XSD));
            unmarshaller.setSchema(schema);

            final StreamSource streamSource = new StreamSource(new StringReader(payload));

            return unmarshaller.unmarshal(streamSource, CP20.class).getValue();

        } catch (JAXBException | SAXException e) {
            throw new BadRequestException("Invalid xml payload:" + e.getCause().getMessage(), e);
        }

    }


    public String getMarshalContent(final CP20 obj) {
        try {
            obj.getDocuments().getDocument().stream().forEach( e -> e.setDocumentContent("".getBytes()));
            final Marshaller marshaller   = jaxbContext.createMarshaller();
            final StringWriter sw = new StringWriter();
            marshaller.marshal(obj, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new BadRequestException("Invalid xml payload:" + e.getCause().getMessage(), e);
        }

    }

}