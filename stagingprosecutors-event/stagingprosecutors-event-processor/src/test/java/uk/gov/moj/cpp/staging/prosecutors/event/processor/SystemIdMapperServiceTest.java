package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.CONFLICT;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMapperServiceTest {


    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private SystemIdMapperService systemIdMapperService;


    private static final String SOURCE_TYPE = "OU_URN";
    private static final String TARGET_TYPE = "CASE_FILE_ID";
    private static final String SPI_SOURCE_TYPE = "SPI-URN";
    private static final String SPI_TARGET_TYPE = "CASE-ID";

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {

        final String caseURN = "11AAACD3456";
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseURN, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCppCaseIdFor(caseURN);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExistsForSPI() {

        final String caseURN = "11AAACD9999";
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseURN, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(caseURN, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCppCaseIdFor(caseURN);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }


    @Test
    public void shouldReturnCaseIdWhenNoMappingExists() {
        final UUID userId = randomUUID();
        final String caseURN = "11AAACD3457";
        ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(caseURN, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));

        final UUID cppCaseId = systemIdMapperService.getCppCaseIdFor(caseURN);

        assertThat("cppCaseId should match", cppCaseId, is(systemIdMapArgumentCaptor.getValue().getTargetId()));
    }

    @Test
    public void shouldReturnExceptionWhenNoMappingExists() throws IllegalStateException {
        final UUID userId = randomUUID();
        final String caseURN = "11AAACD3457";
        final UUID[] cppCaseId = new UUID[1];
        ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(caseURN, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.findBy(caseURN, SPI_SOURCE_TYPE, SPI_TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), CONFLICT, empty()));

        assertThrows(IllegalStateException.class, () -> cppCaseId[0] = systemIdMapperService.getCppCaseIdFor(caseURN));
    }

    @Test
    public void shouldReturnCaseIdForMaterialSubmissionWhenCaseIdMappingWithCaseURN() {

        final String caseURN = "JAB12XZ";
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseURN, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCaseIdForMaterialSubmission(caseURN);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }

    @Test
    public void shouldReturnOtherCaseIdForMaterialSubmissionWhenCaseIdMappingWithCaseURN() {

        final String caseURN = "JAB12XZ";
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseURN, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCppCaseIdFor(caseURN);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }

    @Test
    public void shouldReturnCaseIdForMaterialSubmissionWhenCaseIdMappingWithOUCodeAndCaseURN() {

        final String caseReference = "MNXY:JAB12XZ";
        final UUID userId = randomUUID();
        final UUID mappedCppCaseId = randomUUID();

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), caseReference, "", mappedCppCaseId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseReference, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID cppCaseId = systemIdMapperService.getCaseIdForMaterialSubmission(caseReference);

        assertThat(cppCaseId, is(mappedCppCaseId));
    }

    @Test
    public void shouldReturnCaseIdForMaterialSubmissionWhenNoMappingExists() {
        final UUID userId = randomUUID();
        final String caseURN = "XZ21JAB";
        ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(caseURN, SOURCE_TYPE, TARGET_TYPE, userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));

        final UUID cppCaseId = systemIdMapperService.getCaseIdForMaterialSubmission(caseURN);

        assertThat("cppCaseId should match", cppCaseId, is(systemIdMapArgumentCaptor.getValue().getTargetId()));
    }
}