package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class ContactDetails {

    @Builder.Default
    public String workTelephoneNumber = "+441234121212";

    @Builder.Default
    public String homeTelephoneNumber = "+441234121212";

    @Builder.Default
    public String mobileTelephoneNumber = "+441234121212";

    @Builder.Default
    public String primaryEmail = "aer@af.com";

    @Builder.Default
    public String secondaryEmail = "aer@af.com";

    @Builder.Default
    public String additionalProperty = null;

}
