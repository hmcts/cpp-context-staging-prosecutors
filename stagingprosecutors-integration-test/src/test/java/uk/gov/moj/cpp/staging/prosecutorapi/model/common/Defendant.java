package uk.gov.moj.cpp.staging.prosecutorapi.model.common;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;

import lombok.Builder;

@Builder
public class Defendant {

    @Builder.Default
    public String asn = "arrest/summons";

    @Builder.Default
    public String prosecutorCosts = "20.10";

    @Builder.Default
    public Person defendantPerson = Person.builder().build();

    @Builder.Default
    public Organisation organisation = Organisation.builder().build();

    @Builder.Default
    public String documentationLanguage = Language.E.name();

    @Builder.Default
    public String hearingLanguage = Language.E.name();

    @Builder.Default
    public String languageRequirement = "no special language needs";

    @Builder.Default
    public String specificRequirements = "no special needs";

    @Builder.Default
    public int numPreviousConvictions = 3;

    @Builder.Default
    public Offence[] offences = {Offence.builder().build()};

    @Builder.Default
    public String prosecutorDefendantId = "123";

    @Builder.Default
    public String additionalProperty = null;

}
