package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.ApplicationStatus.IN_PROGRESS;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;

import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.moj.cpp.staging.prosecutorapi.model.event.TestCourtApplicationCreated;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ApplicationResponseIT {

    private final UUID applicationId = randomUUID();

    @Test
    public void shouldSendAddMaterialRequestsToProsecutionCasefileWhenDocumentUnbundleSuccessful() throws SQLException, FileServiceException, InterruptedException {
        final TestCourtApplicationCreated event = getCourtApplicationCreatedPayload();

        event.emitter()
                .addCustomMetadata("applicationId", applicationId.toString())
                .emit();

    }

    private TestCourtApplicationCreated getCourtApplicationCreatedPayload() {
        return TestCourtApplicationCreated.builder()
                .courtApplication(courtApplication().withId(applicationId)
                        .withType(courtApplicationType().withId(randomUUID())
                                .withCode("code").withCategoryCode("catCode")
                                .withType("type").withLinkType(LinkType.LINKED)
                                .withJurisdiction(Jurisdiction.CROWN).withSummonsTemplateType(SummonsTemplateType.GENERIC_APPLICATION)
                                .withBreachType(BreachType.NOT_APPLICABLE).withAppealFlag(FALSE).withPleaApplicableFlag(TRUE).withApplicantAppellantFlag(TRUE)
                                .withPleaApplicableFlag(FALSE).withCommrOfOathFlag(FALSE).withCourtOfAppealFlag(TRUE).withCourtExtractAvlFlag(FALSE)
                                .withProsecutorThirdPartyFlag(TRUE).withSpiOutApplicableFlag(TRUE)
                                .withOffenceActiveOrder(OffenceActiveOrder.COURT_ORDER)
                                .build())
                        .withApplicationReceivedDate("2022-01-01")
                        .withApplicant(courtApplicationParty().withId(randomUUID()).withSummonsRequired(Boolean.TRUE).withNotificationRequired(FALSE).build())
                        .withSubject(courtApplicationParty().withId(randomUUID()).withSummonsRequired(Boolean.TRUE).withNotificationRequired(FALSE).build())
                        .withApplicationStatus(IN_PROGRESS)
                        .build())
                .build();
    }

}
