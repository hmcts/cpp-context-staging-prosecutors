package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class SelfDefinedInformation {

    @Builder.Default
    public String nationality = "E";

    @Builder.Default
    public String additionalNationality = "E";

    @Builder.Default
    public int gender = 1;

    @Builder.Default
    public String ethnicity = "W1";

    @Builder.Default
    public String additionalProperty = null;

}
