package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Objects.nonNull;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted.applicationSubmitted;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ApplicationSubmitted;

import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S1068", "squid:S1450"})
public class ApplicationSubmission implements Aggregate {

    private static final long serialVersionUID = -846244438748619250L;

    private UUID pocaFileId;
    private String senderEmail;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ApplicationSubmitted.class).apply(e -> {
                    if (nonNull(e.getPocaFileId())) {
                        this.pocaFileId = e.getPocaFileId();
                    }
                    if (isNotEmpty(e.getSenderEmail())) {
                        this.senderEmail = e.getSenderEmail();
                    }
                }),
                otherwiseDoNothing());
    }

    public Stream<Object> receiveSubmission(final CourtApplication courtApplication, final BoxHearingRequest boxHearingRequest, final UUID pocaFileId, final String senderEmail, final String emailSubject) {
        return apply(of(applicationSubmitted()
                .withCourtApplication(courtApplication)
                .withBoxHearingRequest(boxHearingRequest)
                .withPocaFileId(pocaFileId)
                .withSenderEmail(senderEmail)
                .withEmailSubject(emailSubject)
                .build()));
    }

}
