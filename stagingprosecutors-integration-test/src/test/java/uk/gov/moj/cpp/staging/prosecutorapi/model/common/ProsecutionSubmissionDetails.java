package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class ProsecutionSubmissionDetails {

    @Builder.Default
    public String prosecutingAuthority = "GAEAA01";

    @Builder.Default
    public String urn = "TVL12345";

    @Builder.Default
    public String informant = "Adam";

    @Builder.Default
    public String writtenChargePostingDate = "2018-03-20";

    @Builder.Default
    public String additionalProperty = null;

}
