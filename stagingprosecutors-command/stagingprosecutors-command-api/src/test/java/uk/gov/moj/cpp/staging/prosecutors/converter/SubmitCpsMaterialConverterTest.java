package uk.gov.moj.cpp.staging.prosecutors.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.staging.prosecutors.cps.schema.Cp20ObjectUnMarshaller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.cgi.cp.cp20._2020_03.CP20;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitCpsMaterialCommand;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitCpsMaterialConverterTest {

    private static final String CP20_XML_FILE_NAME = "CP20.xml";

    @Spy
    Cp20ObjectUnMarshaller cp20ObjectUnMarshaller;

    @InjectMocks
    SubmitCpsMaterialConverter submitCpsMaterialConverter;

    @Test
    public void shouldConvertCp20ToSubmitMaterialCommand() throws IOException {
        final String xmlPayload = FileUtils.readFileToString(new File(Thread.currentThread().getContextClassLoader().getResource(CP20_XML_FILE_NAME).getFile()));
        final CP20 cp20 = cp20ObjectUnMarshaller.getCP20Type(xmlPayload);
        final List<UUID> fileStoreIds = Arrays.asList(randomUUID(), randomUUID());
        final SubmitCpsMaterialCommand submitCpsMaterialCommand = submitCpsMaterialConverter.convert(cp20, fileStoreIds);


        assertThat(submitCpsMaterialCommand.getTransactionID(), is(29990));
        assertThat(submitCpsMaterialCommand.getCompassCaseId(), is(6301));
        assertThat(submitCpsMaterialCommand.getUrn(), is("45GD4371025"));
        assertThat(submitCpsMaterialCommand.getResponseEmail(), is("SurreyCJU@logica.com"));

        assertThat(submitCpsMaterialCommand.getDocuments().size(), is(2));
        assertThat(submitCpsMaterialCommand.getDocuments().get(0).getDocumentId(), is("D372133"));
        assertThat(submitCpsMaterialCommand.getDocuments().get(0).getFileStoreId(), is(fileStoreIds.get(0)));
        assertThat(submitCpsMaterialCommand.getDocuments().get(1).getDocumentId(), is("D372138"));
        assertThat(submitCpsMaterialCommand.getDocuments().get(1).getFileStoreId(), is(fileStoreIds.get(1)));



        assertThat(submitCpsMaterialCommand.getDefendants().size(), is(2));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getAsn(), is("1011PP0000000500001B"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getSurname(), is("TWOOWESEVEN"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getForenames(), is("Jack"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getDob().toString(), is("1999-01-01"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getOuCode(), is("B13CC00"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(0).getDefendantID(), is("11235617"));
        assertThat(submitCpsMaterialCommand.getDefendants().get(1).getDefendantID(), is("11235757"));
    }
}