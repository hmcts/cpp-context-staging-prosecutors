package service;


import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Jurisdiction.EITHER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationRequestNotification;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationStatus;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.dto.CourtApplicationWithCaseDto;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsPayloadTransformServiceTest {

    public static final String APPLICATION_REQUEST_STATUS = "application-request-status";
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CpsPayloadTransformService cpsPayloadTransformService;

    @BeforeEach
    public void setUp() {
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldTransformApplicationCreatedNotificationWithSubject() {
        final JsonObject payload = getPayload("json/public.progression.court-application-created-with-subject.json", randomUUID());
        final CourtApplicationWithCaseDto courtApplicationCreatedDto = jsonToObjectConverter.convert(payload, CourtApplicationWithCaseDto.class);
        final JsonObject actual = cpsPayloadTransformService.transformCourtApplicationNotification(courtApplicationCreatedDto, "application-created", Collections.singletonList(UUID.fromString("5f045b6a-241b-3a37-9776-bb584c9dd73d")));
        final String notificationType  = actual.getString("notificationType");
        final JsonObject courtApplicationObject = actual.getJsonObject("applicationNotification").getJsonObject("courtApplication");
        final JsonObject courtApplicationCases = courtApplicationObject.getJsonArray("courtApplicationCases").getJsonObject(0);
        final String caseURN = courtApplicationCases.getString("caseURN");
        final String prosecutorOUCode = courtApplicationCases.getString("prosecutorOUCode");
        final String applicationId = courtApplicationObject.getString("applicationId");
        final String courtApplicationTypeCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("code");
        final String categoryCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("categoryCode");
        final String jurisdiction = courtApplicationObject.getJsonObject("courtApplicationType").getString("jurisdiction");

        assertThat(notificationType,is("application-created") );
        assertThat(applicationId, is("4f63c60c-77a4-4c59-b518-061a7d4abd7f"));
        assertThat(caseURN, is("72GD9544122"));
        assertThat(prosecutorOUCode, is("0530000"));
        assertThat(categoryCode, is("CO"));
        assertThat(courtApplicationTypeCode, is("MC80518"));
        assertThat(jurisdiction, is(EITHER.toString()));
    }

    @Test
    public void shouldTransformApplicationCreatedNotificationWithApplicant() {
        final JsonObject payload = getPayload("json/public.progression.court-application-created-with-applicant.json", randomUUID());
        final CourtApplicationWithCaseDto courtApplicationCreatedDto = jsonToObjectConverter.convert(payload, CourtApplicationWithCaseDto.class);
        final JsonObject actual = cpsPayloadTransformService.transformCourtApplicationNotification(courtApplicationCreatedDto, "application-created", Collections.singletonList(UUID.fromString("5f045b6a-241b-3a37-9776-bb584c9dd73d")));
        final String notificationType  = actual.getString("notificationType");
        final JsonObject courtApplicationObject = actual.getJsonObject("applicationNotification").getJsonObject("courtApplication");
        final JsonObject courtApplicationCases = courtApplicationObject.getJsonArray("courtApplicationCases").getJsonObject(0);
        final String caseURN = courtApplicationCases.getString("caseURN");
        final String prosecutorOUCode = courtApplicationCases.getString("prosecutorOUCode");
        final String applicationId = courtApplicationObject.getString("applicationId");
        final String courtApplicationTypeCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("code");
        final String categoryCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("categoryCode");
        final String jurisdiction = courtApplicationObject.getJsonObject("courtApplicationType").getString("jurisdiction");

        assertThat(notificationType,is("application-created") );
        assertThat(applicationId, is("4f63c60c-77a4-4c59-b518-061a7d4abd7f"));
        assertThat(caseURN, is("72GD9544122"));
        assertThat(prosecutorOUCode, is("0530000"));
        assertThat(categoryCode, is("CO"));
        assertThat(courtApplicationTypeCode, is("MC80518"));
        assertThat(jurisdiction, is(EITHER.toString()));
    }

    @Test
    public void shouldTransformApplicationCreatedMultipLeRespondentsNotification() {
        final JsonObject payload = getPayload("json/public.progression.court-application-created-multiple-respondents.json", randomUUID());
        final CourtApplicationWithCaseDto courtApplicationCreatedDto = jsonToObjectConverter.convert(payload, CourtApplicationWithCaseDto.class);
        final JsonObject actual = cpsPayloadTransformService.transformCourtApplicationNotification(courtApplicationCreatedDto, "application-created", Collections.singletonList(UUID.fromString("5f045b6a-241b-3a37-9776-bb584c9dd73d")));
        final String notificationType  = actual.getString("notificationType");
        final JsonObject courtApplicationObject = actual.getJsonObject("applicationNotification").getJsonObject("courtApplication");
        final JsonObject courtApplicationCases = courtApplicationObject.getJsonArray("courtApplicationCases").getJsonObject(0);
        final String caseURN = courtApplicationCases.getString("caseURN");
        final String prosecutorOUCode = courtApplicationCases.getString("prosecutorOUCode");
        final String applicationId = courtApplicationObject.getString("applicationId");
        final String courtApplicationTypeCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("code");
        final String categoryCode = courtApplicationObject.getJsonObject("courtApplicationType").getString("categoryCode");
        final String jurisdiction = courtApplicationObject.getJsonObject("courtApplicationType").getString("jurisdiction");

        assertThat(notificationType, is("application-created"));
        assertThat(applicationId, is("88ab9be4-45f3-4ad4-8f44-46c29f3ec6f4"));
        assertThat(caseURN, is("72GD9544122"));
        assertThat(prosecutorOUCode, is("0530000"));
        assertThat(categoryCode, is("CO"));
        assertThat(courtApplicationTypeCode, is("MC80518"));
        assertThat(jurisdiction, is(EITHER.toString()));
    }

    @Test
    public void shouldTransformApplicationRequestNotification() {
        final UUID applicationId = randomUUID();
        final ApplicationRequestNotification applicationRequestNotification = ApplicationRequestNotification.applicationRequestNotification()
                .withApplicationId(applicationId.toString())
                .withApplicationStatus(ApplicationStatus.CREATED.name())
                .build();
        final JsonObject actual = cpsPayloadTransformService.transformApplicationRequestNotification(applicationRequestNotification, APPLICATION_REQUEST_STATUS);


        assertThat(actual.getString("notificationDate"), notNullValue());
        assertThat(actual.getString("notificationType"), is(APPLICATION_REQUEST_STATUS));
        assertThat(actual.getJsonObject("applicationRequestNotification").getString("applicationId"), is(applicationRequestNotification.getApplicationId()));
        assertThat(actual.getJsonObject("applicationRequestNotification").getString("applicationStatus"), is(applicationRequestNotification.getApplicationStatus()));
    }

    private static JsonObject getPayload(final String path, final UUID hearingId) {
        String request = null;
        try {
            final InputStream inputStream = CpsPayloadTransformServiceTest.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, IsNull.notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset()).replace("HEARING_ID", hearingId.toString());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        final JsonReader reader = createReader(new StringReader(request));
        return reader.readObject();
    }
}