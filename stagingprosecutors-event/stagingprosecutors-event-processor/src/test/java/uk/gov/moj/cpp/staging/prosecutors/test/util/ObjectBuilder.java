package uk.gov.moj.cpp.staging.prosecutors.test.util;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed.submitApplicationValidationFailed;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Error;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ErrorDetails;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ValidationError;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2187"})
public class ObjectBuilder {
    public static final String SESSION_ID = randomUUID().toString();
    public static final String USER_ID = randomUUID().toString();
    public static final UUID STREAM_ID = randomUUID();
    public static final UUID CASE_ID = randomUUID();
    public static final String ASN = "ASN";

    public static Metadata getMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withSessionId(SESSION_ID)
                .withUserId(USER_ID)
                .withStreamId(STREAM_ID)
                .build();
    }

    public static SubmitApplicationValidationFailed buildSubmitApplicationValidationFailed(final ErrorDetails errorDetails,final ApplicationSubmitted applicationSubmitted) {
        return submitApplicationValidationFailed()
                .withApplicationSubmitted(applicationSubmitted)
                .withErrorDetails(errorDetails)
                .build();
    }

    public static ErrorDetails buildErrorDetails(final List<ValidationError> validationErrorList) {
        List<Error> errorList = new ArrayList<>();
        final Error firstError = Error.error()
                .withErrorCode(validationErrorList.get(0).getCode())
                .withErrorDescription(validationErrorList.get(0).getText())
                .build();
        final Error secondError = Error.error()
                .withErrorCode(validationErrorList.get(1).getCode())
                .withErrorDescription(validationErrorList.get(1).getText())
                .build();
        errorList.add(firstError);
        errorList.add(secondError);
        return ErrorDetails.errorDetails()
                .withErrorDetails(errorList)
                .build();
    }
}
