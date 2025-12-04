package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import lombok.Builder;

@Builder
public class Person {

    @Builder.Default
    public String title = "Mr";

    @Builder.Default
    public String forename = "John";

    @Builder.Default
    public String forename2 = "Adam";

    @Builder.Default
    public String forename3 = "Theodore";

    @Builder.Default
    public String surname = "SMITH";

    @Builder.Default
    public String dateOfBirth = "1970-02-03";

    @Builder.Default
    public String occupation = "TA";

    @Builder.Default
    public int occupationCode = 666;

    @Builder.Default
    public SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.builder().build();

    @Builder.Default
    public String driverNumber = "KER99861065SE9RM";

    @Builder.Default
    public Address address = Address.builder().build();

    @Builder.Default
    public String nationalInsuranceNumber = "AB123456A";

    @Builder.Default
    public ContactDetails contactDetails = ContactDetails.builder().build();

    @Builder.Default
    public String additionalProperty = null;

}
