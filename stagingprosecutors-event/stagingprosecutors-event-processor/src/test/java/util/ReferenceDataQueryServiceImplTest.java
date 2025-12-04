package util;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CppGroup;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReadUserGroups;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ReferenceDataQueryServiceImpl;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataQueryServiceImplTest {

    public static final String ID = "id";
    public static final String PROSECUTORS = "prosecutors";
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private ReferenceDataQueryServiceImpl referenceDataQueryService;

    @Mock
    private Requester requester;


    @Test
    public void shouldGetDocumentTypeAccessBySectionCode() {
        final String sectionCode = "1";
        final DocumentTypeAccessReferenceData queryResponseDocumentTypeAccessReferenceData = DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                .withId(randomUUID())
                .withActionRequired(true)
                .withCourtDocumentTypeRBAC(CourtDocumentTypeRBAC.courtDocumentTypeRBAC()
                        .withReadUserGroups(singletonList(ReadUserGroups.readUserGroups()
                                .withCppGroup(CppGroup.cppGroup()
                                        .withId(randomUUID())
                                        .withGroupName("groupName")
                                        .build())
                                .build()))
                        .build())
                .withDocumentCategory("docCat1")
                .withSection("sec1")
                .build();
        final Envelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("referencedata.query.document-type-access-by-sectioncode").build(), objectToJsonObjectConverter.convert(queryResponseDocumentTypeAccessReferenceData));
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = referenceDataQueryService.getDocumentTypeAccessBySectionCode(envelope.metadata(), sectionCode);

        assertThat(documentTypeAccessReferenceData.getId(), is(queryResponseDocumentTypeAccessReferenceData.getId()));
        assertThat(documentTypeAccessReferenceData.getActionRequired(), is(queryResponseDocumentTypeAccessReferenceData.getActionRequired()));
        assertThat(documentTypeAccessReferenceData.getCourtDocumentTypeRBAC().getReadUserGroups().get(0).getCppGroup().getId(), is(queryResponseDocumentTypeAccessReferenceData.getCourtDocumentTypeRBAC().getReadUserGroups().get(0).getCppGroup().getId()));
        assertThat(documentTypeAccessReferenceData.getCourtDocumentTypeRBAC().getReadUserGroups().get(0).getCppGroup().getGroupName(), is(queryResponseDocumentTypeAccessReferenceData.getCourtDocumentTypeRBAC().getReadUserGroups().get(0).getCppGroup().getGroupName()));
        assertThat(documentTypeAccessReferenceData.getDocumentCategory(), is(queryResponseDocumentTypeAccessReferenceData.getDocumentCategory()));
        assertThat(documentTypeAccessReferenceData.getSection(), is(queryResponseDocumentTypeAccessReferenceData.getSection()));

    }

    @Test
    public void shouldGetParentBundleSectionByCpsBundleCode() {
        final String cpsBundleCode = "1";
        final ParentBundleSectionReferenceData parentBundleSectionReferenceData = ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(randomUUID())
                .withCpsBundleCode(cpsBundleCode)
                .withTargetSectionCode("TargetSectionCode")
                .build();
        final Envelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("referencedata.query.parent-bundle-section").build(), objectToJsonObjectConverter.convert(parentBundleSectionReferenceData));
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final ParentBundleSectionReferenceData parentBundleSectionByCpsBundleCode = referenceDataQueryService.getParentBundleSectionByCpsBundleCode(envelope.metadata(), cpsBundleCode);

        assertThat(parentBundleSectionByCpsBundleCode.getId(), is(parentBundleSectionReferenceData.getId()));
        assertThat(parentBundleSectionByCpsBundleCode.getCpsBundleCode(), is(parentBundleSectionReferenceData.getCpsBundleCode()));
        assertThat(parentBundleSectionByCpsBundleCode.getTargetSectionCode(), is(parentBundleSectionReferenceData.getTargetSectionCode()));

    }

    @Test
    public void shouldGetCPSProsecutors() {
        final UUID id1 = randomUUID();
        final UUID id2 = randomUUID();
        final JsonObject responsePayload = Json.createObjectBuilder()
                .add(PROSECUTORS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(ID, id1.toString())
                                .build())
                        .add(Json.createObjectBuilder()
                                .add(ID, id2.toString())
                                .build())
                        .build())
                .build();
        final JsonEnvelope envelope = envelopeFrom(Envelope.metadataBuilder().withId(randomUUID()).withName("referencedata.query.get.prosecutor.by.cpsflag").build(), responsePayload);
        when(requester.request(any())).thenReturn(envelope);
        final Optional<JsonArray> cpsProsecutors = referenceDataQueryService.getCPSProsecutors(envelope, requester);

        assertThat(cpsProsecutors.get().getJsonObject(0).getString(ID), is(id1.toString()));
        assertThat(cpsProsecutors.get().getJsonObject(1).getString(ID), is(id2.toString()));

    }

    public Metadata getMetaData() {
        return Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("actionName")
                .createdAt(ZonedDateTime.now())
                .build();
    }

}