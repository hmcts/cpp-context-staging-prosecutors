package uk.gov.moj.cpp.staging.prosecutors.command.handler.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;

public interface ReferenceDataService {
    public Optional<CourtApplicationType> retrieveApplicationTypes(final String code);
    public List<LocalDate> getPublicHolidays(final LocalDate fromDate,
                                             final LocalDate toDate);
}
