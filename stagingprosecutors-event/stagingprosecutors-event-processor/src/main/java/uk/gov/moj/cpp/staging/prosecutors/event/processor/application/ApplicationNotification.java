package uk.gov.moj.cpp.staging.prosecutors.event.processor.application;

import uk.gov.justice.core.courts.CourtApplication;

public class ApplicationNotification {
    final CourtApplication courtApplication;

    public ApplicationNotification(final CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }
    public static Builder applicationNotification() {
        return new Builder();
    }

    public static class Builder {
        private CourtApplication courtApplication;

        public Builder withCourtApplication(final CourtApplication courtApplication) {
            this.courtApplication = courtApplication;
            return this;
        }

        public Builder withValuesFrom(final ApplicationNotification applicationNotification) {
            this.courtApplication = applicationNotification.getCourtApplication();
            return this;
        }

        public ApplicationNotification build() {
            return new ApplicationNotification(courtApplication);
        }
    }
}
