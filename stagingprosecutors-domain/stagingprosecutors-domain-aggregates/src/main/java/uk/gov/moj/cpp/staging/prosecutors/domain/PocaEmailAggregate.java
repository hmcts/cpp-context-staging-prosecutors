package uk.gov.moj.cpp.staging.prosecutors.domain;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentNotValidated.pocaDocumentNotValidated;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentValidated.pocaDocumentValidated;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Errors;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PocaDocumentNotValidated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("squid:MethodCyclomaticComplexity")
public class PocaEmailAggregate implements Aggregate {

    @Override
    public Object apply(Object event) {
        return match(event).with(
                otherwiseDoNothing()
        );
    }

    public Stream<Object> pocaEmailValidated(final UUID pocaFileId, final String senderEmail, final String emailSubject, final CourtApplication courtApplication, final BoxHearingRequest boxHearingRequest) {
        return apply(Stream.of(pocaDocumentValidated()
                .withCourtApplication(courtApplication)
                .withBoxHearingRequest(boxHearingRequest)
                .withPocaFileId(pocaFileId)
                .withSenderEmail(senderEmail)
                .withEmailSubject(emailSubject)
                .build()));
    }

    public Stream<Object> pocaEmailNotValidated(final UUID pocaFileId, final String senderEmail, final String emailSubject, final List<String> validateStructuredData) {

        final PocaDocumentNotValidated.Builder builder = pocaDocumentNotValidated()
                .withEmailSubject(emailSubject)
                .withPocaFileId(pocaFileId)
                .withSenderEmail(senderEmail);
        final List<Errors> errorsList = new ArrayList<>();
        validateStructuredData.forEach(error ->
                errorsList.add(Errors.errors().withErrorCode(error)
                        .build()));
        builder.withErrors(errorsList);
        return apply(Stream.of(builder.build()));
    }

}
