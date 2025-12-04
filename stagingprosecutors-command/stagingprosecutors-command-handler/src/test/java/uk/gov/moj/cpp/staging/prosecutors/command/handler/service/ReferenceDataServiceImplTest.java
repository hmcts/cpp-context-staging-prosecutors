package uk.gov.moj.cpp.staging.prosecutors.command.handler.service;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.staging.prosecutors.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.staging.prosecutors.test.utils.FileResourceObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceImplTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataServiceImpl referenceDataServiceImpl;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Test
    public void shouldReturnApplicationType() throws IOException {
        final String code = "CC13501";

        final Envelope<JsonObject> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.charge-prosecution", UUID.randomUUID()),
                        handlerTestHelper.convertFromFile("json/application-types.json", JsonObject.class));

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);


        final Optional<CourtApplicationType> applicationType =  referenceDataServiceImpl.retrieveApplicationTypes(code);

        assertThat(applicationType.isPresent(), is(true));
        assertThat(applicationType.get().getCategoryCode(), is("CR"));
        assertThat(applicationType.get().getCode(), is(code));
        assertThat(applicationType.get().getJurisdiction().toString(), is("CROWN"));
    }

    @Test
    public void shouldReturnPublicHolidays() throws IOException {

        final LocalDate fromDate = LocalDate.now();
        final LocalDate toDate = LocalDate.now().plusDays(30);

        final Envelope<JsonObject> envelope =
                envelopeFrom(metadataFor("stagingprosecutors.command.charge-prosecution", UUID.randomUUID()),
                        handlerTestHelper.convertFromFile("json/public-holidays.json", JsonObject.class));

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);

        List<LocalDate> dates =  referenceDataServiceImpl.getPublicHolidays(fromDate, toDate);

        assertThat(dates.size(), is(3));
        assertThat(dates.get(0), is(LocalDate.parse("2022-08-29")));
        assertThat(dates.get(1), is(LocalDate.parse("2022-12-26")));
        assertThat(dates.get(2), is(LocalDate.parse("2022-12-27")));

    }


}
