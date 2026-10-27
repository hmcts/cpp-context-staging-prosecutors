package uk.gov.moj.cpp.staging.prosecutors.cps.schema;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * J17/J25 parity characterisation (J17 golden master), SP-M04.
 *
 * The CPS XML material endpoint unmarshals the CP20 payload against cp20.xsd, then re-marshals it without the file
 * content and forwards that XML downstream. The J25 upgrade moves to Jakarta XML Binding 4 with classes regenerated
 * by jaxb-maven-plugin, so pin the exact re-marshalled XML (namespace prefixes, element order, date formats, empty
 * elements) and the exact 400 messages for schema-invalid payloads, which come from the JDK's schema validator.
 */
class Cp20XmlParityTest {

    private final Cp20ObjectUnMarshaller cp20ObjectUnMarshaller = new Cp20ObjectUnMarshaller();

    @Test
    void spM04_shouldReMarshalTheJ17Cp20Xml() throws IOException {
        final String xmlPayload = readResource("CP20_CGI.xml");

        final String marshalled = cp20ObjectUnMarshaller.getMarshalContent(cp20ObjectUnMarshaller.getCP20Type(xmlPayload));

        assertThat(marshalled, is(readResource("parity/expected-cp20-marshalled.xml")));
    }

    @Test
    void spM04_shouldRejectMaterialTypeBelowRangeWithTheJ17Message() throws IOException {
        assertBadRequest(readResource("CP20_CGI_INVALID_MATERIAL_TYPE.xml"),
                "Invalid xml payload:cvc-minInclusive-valid: Value '-1' is not facet-valid with respect to minInclusive '0' for type '#AnonType_MaterialTypeTDocument'.");
    }

    @Test
    void spM04_shouldRejectMaterialTypeAboveRangeWithTheJ17Message() throws IOException {
        assertBadRequest(readResource("CP20_CGI_INVALID_MATERIAL_2_TYPE.xml"),
                "Invalid xml payload:cvc-maxInclusive-valid: Value '23' is not facet-valid with respect to maxInclusive '22' for type '#AnonType_MaterialTypeTDocument'.");
    }

    @Test
    void spM04_shouldRejectUnknownRootElementWithTheJ17Message() {
        assertBadRequest("<test></test>", "Invalid xml payload:cvc-elt.1.a: Cannot find the declaration of element 'test'.");
    }

    @Test
    void spM04_shouldRejectNonXmlWithTheJ17Message() {
        assertBadRequest("not xml", "Invalid xml payload:Content is not allowed in prolog.");
    }

    private void assertBadRequest(final String xmlPayload, final String expectedMessage) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> cp20ObjectUnMarshaller.getCP20Type(xmlPayload));
        assertThat(exception.getMessage(), is(expectedMessage));
    }

    private static String readResource(final String path) throws IOException {
        try (InputStream inputStream = Cp20XmlParityTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(inputStream.readAllBytes(), UTF_8);
        }
    }
}
