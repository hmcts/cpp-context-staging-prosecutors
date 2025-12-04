package uk.gov.moj.cpp.staging.prosecutors.event.processor.converter;

import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language.valueFor;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOffence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SjpPerson;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SjpDefendantToProsecutionCaseFileDefendantConverter implements Converter<SjpDefendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> {

    final LocalDate postingDate;
    private final Converter<SjpPerson, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual> prosecutionPersonToIndividualConverter
            = new SjpPersonToProsecutionCaseFileIndividualConverter();
    private final Converter<List<SjpOffence>, List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence>> prosecutionOffenceToProsecutionCaseFileOffenceConverter
            = new SjpProsecutionOffenceToProsecutionCaseFileOffenceConverter();

    public SjpDefendantToProsecutionCaseFileDefendantConverter(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    @Override
    public uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant convert(final SjpDefendant defendant) {
        final Optional<SjpPerson> personOpt = ofNullable(defendant.getDefendantPerson());
        final Optional<SjpOrganisation> sjpOrganisationOpt = ofNullable(defendant.getOrganisation());

        Defendant.Builder defendantBuilder = defendant()
                .withAsn(defendant.getAsn())
                .withDocumentationLanguage(convertLang(defendant.getDocumentationLanguage()))
                .withHearingLanguage(convertLang(defendant.getHearingLanguage()))
                .withId(randomUUID().toString())
                .withProsecutorDefendantReference(defendant.getProsecutorDefendantId())
                .withLanguageRequirement(defendant.getLanguageRequirement())
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withNumPreviousConvictions(defendant.getNumPreviousConvictions())
                .withOffences(prosecutionOffenceToProsecutionCaseFileOffenceConverter.convert(defendant.getOffences()))
                .withPostingDate(postingDate)
                .withAppliedProsecutorCosts(new BigDecimal(valueOf(defendant.getProsecutorCosts())));

        personOpt.ifPresent(sjpPerson -> defendantBuilder.withIndividual(prosecutionPersonToIndividualConverter.convert(sjpPerson)));

        sjpOrganisationOpt.ifPresent(sjpOrganisation -> {
            defendantBuilder.withOrganisationName(sjpOrganisation.getOrganisationName())
                    .withAddress(convertAddress(sjpOrganisation.getAddress()));

            final Optional<ContactDetails> contactOpt = ofNullable(sjpOrganisation.getContactDetails());
            contactOpt.ifPresent(contactDetails -> {
                defendantBuilder.withEmailAddress1(contactDetails.getPrimaryEmail());
                defendantBuilder.withEmailAddress2(contactDetails.getSecondaryEmail());
                defendantBuilder.withTelephoneNumberBusiness(contactDetails.getWorkTelephoneNumber());
            });
        });

        return defendantBuilder.build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address convertAddress(final Address address) {
        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.Builder builder = address()
                .withAddress1(address.getAddress1());

        ofNullable(address.getAddress2()).ifPresent(builder::withAddress2);
        ofNullable(address.getAddress3()).ifPresent(builder::withAddress3);
        ofNullable(address.getAddress4()).ifPresent(builder::withAddress4);
        ofNullable(address.getAddress5()).ifPresent(builder::withAddress5);
        ofNullable(address.getPostcode()).ifPresent(builder::withPostcode);

        return builder.build();
    }

    private static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language convertLang(
            final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language dl) {
        return valueFor(dl.name()).orElse(null);
    }
}
