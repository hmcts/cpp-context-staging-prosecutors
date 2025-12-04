package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class Organisation {

    @Builder.Default
    public String organisationName = "";

    @Builder.Default
    public String telephoneNumber = "";

}
