package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class Address {

    @Builder.Default
    public String address1 = "2 The Street";

    @Builder.Default
    public String address2 = "address line 2";

    @Builder.Default
    public String address3 = "address line 3";

    @Builder.Default
    public String address4 = "address line 4";

    @Builder.Default
    public String address5 = "address line 5";

    @Builder.Default
    public String postcode = "M1 1AA";

    @Builder.Default
    public String additionalProperty = null;

}

