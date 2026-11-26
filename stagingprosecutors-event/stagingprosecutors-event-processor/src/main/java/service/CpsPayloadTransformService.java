package service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationRequestNotification;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.dto.CourtApplicationWithCaseDto;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.DateConverter;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class CpsPayloadTransformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsPayloadTransformService.class);
    public static final String IS_SUBJECT = "isSubject";
    public static final String APPLICANT = "applicant";
    public static final String RESPONDENTS = "respondents";
    public static final String OUT_OF_TIME_REASON = "outOfTimeReason";
    public static final String COURT_APPLICATION = "courtApplication";
    public static final String NAME = "name";
    public static final String BOX_HEARING_REQUEST = "boxHearingRequest";
    public static final String JURISDICTION_TYPE = "jurisdictionType";
    public static final String APPLICATION_DUE_DATE = "applicationDueDate";
    public static final String SEND_APPOINTMENT_LETTER = "sendAppointmentLetter";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String TITLE = "title";
    public static final String FIRST_NAME = "firstName";
    public static final String MIDDLE_NAME = "middleName";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String LAST_NAME = "lastName";
    public static final String PERSON_DETAILS = "personDetails";
    public static final String REGISTERED_CHARITY_NUMBER = "registeredCharityNumber";
    public static final String INCORPORATION_NUMBER = "incorporationNumber";
    public static final String ORGANISATION = "organisation";
    public static final String APPOINTMENT_NOTIFICATION_REQUIRED = "appointmentNotificationRequired";
    public static final String NOTIFICATION_REQUIRED = "notificationRequired";
    public static final String SUMMONS_REQUIRED = "summonsRequired";
    public static final String CSP_DEFENDANT_ID = "cspDefendantId";
    public static final String ASSOCIATED_PERSONS = "associatedPersons";
    public static final String ROLE = "role";
    public static final String ASN = "asn";
    public static final String PRIMARY_EMAIL = "primaryEmail";
    public static final String SECONDARY_EMAIL = "secondaryEmail";
    public static final String FAX = "fax";
    public static final String HOME = "home";
    public static final String MOBILE = "mobile";
    public static final String WORK = "work";
    public static final String CONTACT_NUMBER = "contactNumber";
    public static final String ADDRESS_1 = "address1";
    public static final String ADDRESS_2 = "address2";
    public static final String ADDRESS_3 = "address3";
    public static final String ADDRESS_4 = "address4";
    public static final String ADDRESS_5 = "address5";
    public static final String POST_CODE = "postCode";
    public static final String ADDRESS = "address";
    public static final String CASE_URN = "caseURN";
    public static final String PROSECUTOR_OU_CODE = "prosecutorOUCode";
    public static final String PERSON = "person";
    public static final String ORGANISATION_PERSONS = "organisationPersons";
    public static final String IS_FEE_PAID = "isFeePaid";
    public static final String IS_FEE_EXEMPT = "isFeeExempt";
    public static final String IS_FEE_UNDERTAKING_ATTACHED = "isFeeUndertakingAttached";
    public static final String PAYMENT_REFERENCE = "paymentReference";
    public static final String COURT_APPLICATION_PAYMENT = "courtApplicationPayment";
    public static final String APPLICATION_ID = "applicationId";
    public static final String COURT_APPLICATION_CASES = "courtApplicationCases";
    public static final String CODE = "code";
    public static final String CATEGORY_CODE = "categoryCode";
    public static final String JURISDICTION = "jurisdiction";
    public static final String COURT_APPLICATION_TYPE = "courtApplicationType";
    public static final String APPLICATION_PARTICULARS = "applicationParticulars";
    public static final String APPLICATION_DECISION_SOUGHT_BY_DATE = "applicationDecisionSoughtByDate";
    public static final String NOTIFICATION_DATE = "notificationDate";
    public static final String NOTIFICATION_TYPE = "notificationType";
    public static final String APPLICATION_NOTIFICATION = "applicationNotification";
    public static final String APPLICATION_REQUEST_NOTIFICATION = "applicationRequestNotification";
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public JsonObject transformCourtApplicationNotification(final CourtApplicationWithCaseDto courtApplicationWithCaseDto, final String notificationType, final List<UUID> cpsProsecutorsList) {
        return createObjectBuilder()
                .add(NOTIFICATION_DATE, DateConverter.getUTCZonedDateTimeString(new Date()))
                .add(NOTIFICATION_TYPE, notificationType)
                .add(APPLICATION_NOTIFICATION, getApplicationNotification(courtApplicationWithCaseDto, cpsProsecutorsList))
                .build();
    }

    public JsonObject transformApplicationRequestNotification(final ApplicationRequestNotification applicationRequestNotification, final String notificationType) {
        return createObjectBuilder()
                .add(NOTIFICATION_DATE, DateConverter.getUTCZonedDateTimeString(new Date()))
                .add(NOTIFICATION_TYPE, notificationType)
                .add(APPLICATION_REQUEST_NOTIFICATION, objectToJsonObjectConverter.convert(applicationRequestNotification))
                .build();
    }

    private JsonObject getApplicationNotification(final CourtApplicationWithCaseDto courtApplicationWithCaseDto, final List<UUID> cpsProsecutorsList) {
        final CourtApplication courtApplication = courtApplicationWithCaseDto.getCourtApplication();
        final BoxHearingRequest boxHearing = courtApplicationWithCaseDto.getBoxHearing();
        final List<ProsecutionCase> prosecutionCases = courtApplicationWithCaseDto.getProsecutionCases();
        final Optional<String> applicationParticulars = ofNullable(courtApplication.getApplicationParticulars());
        final Optional<String> applicationDecisionSoughtByDate = ofNullable(courtApplication.getApplicationDecisionSoughtByDate());

        final Optional<String> code = ofNullable(courtApplication.getType().getCode());
        final JsonObjectBuilder courtApplicationBuilder = createObjectBuilder();
        final JsonObjectBuilder applicationTypeBuilder = createObjectBuilder();
        buildCourtApplicationAndType(courtApplication, code, courtApplicationBuilder, applicationTypeBuilder);
        final JsonObjectBuilder courtApplicationPaymentBuilder = createObjectBuilder();

        applicationParticulars.ifPresent(appParticulars -> courtApplicationBuilder.add(APPLICATION_PARTICULARS, appParticulars));

        buildCourtApplicationTypeAndPayment(courtApplication, courtApplicationBuilder, courtApplicationPaymentBuilder);
        applicationDecisionSoughtByDate.ifPresent(s -> courtApplicationBuilder.add(APPLICATION_DECISION_SOUGHT_BY_DATE, applicationDecisionSoughtByDate.get()));


        final JsonObjectBuilder applicantBuilder = createObjectBuilder();
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        buildApplicantWhenApplicantIsADefendant(cpsProsecutorsList, courtApplication, prosecutionCases, applicantBuilder, applicant);


        final Optional<MasterDefendant> masterDefendantApplicantOpt = ofNullable(applicant.getMasterDefendant());
        final Optional<MasterDefendant> masterDefendantSubjectOpt = ofNullable(courtApplication.getSubject().getMasterDefendant());
        final boolean isSubject = ofNullable(courtApplication.getSubject().getProsecutingAuthority()).isPresent();
        buildApplicantIfApplicantIsOrganisation(applicantBuilder, applicant);

        if (isSubject) {
            ofNullable(applicant.getProsecutingAuthority()).ifPresent(applCant -> applicantBuilder.add(IS_SUBJECT, applCant.getProsecutionAuthorityId().equals(ofNullable(courtApplication.getSubject().getProsecutingAuthority()).get().getProsecutionAuthorityId())));
            if (!ofNullable(applicant.getProsecutingAuthority()).isPresent()) {
                applicantBuilder.add(IS_SUBJECT, false);
            }
        } else if (masterDefendantApplicantOpt.isPresent() && masterDefendantSubjectOpt.isPresent()) {
            final Optional<UUID> defendantIdApOpt = ofNullable(masterDefendantApplicantOpt.get().getMasterDefendantId());
            final Optional<UUID> defendantIdSubOpt = ofNullable(masterDefendantSubjectOpt.get().getMasterDefendantId());
            if (defendantIdApOpt.isPresent() && defendantIdSubOpt.isPresent()) {
                applicantBuilder.add(IS_SUBJECT, defendantIdApOpt.get().equals(defendantIdSubOpt.get()));
            } else {
                applicantBuilder.add(IS_SUBJECT, false);
            }
        } else {
            applicantBuilder.add(IS_SUBJECT, false);
        }

        courtApplicationBuilder.add(APPLICANT, applicantBuilder);
        courtApplicationBuilder.add(RESPONDENTS, getRespondents(courtApplicationWithCaseDto, cpsProsecutorsList).build());
        final List<CourtApplicationParty> thirdParties = courtApplicationWithCaseDto.getCourtApplication().getThirdParties();
        if (nonNull(thirdParties) && !thirdParties.isEmpty()) {
            final JsonArrayBuilder thirdPartyArrayBuilder = createArrayBuilder();
            thirdParties.forEach(courtApplicationParty -> {
                final JsonObjectBuilder thirdPartyBuilder = createObjectBuilder();
                ofNullable(courtApplicationParty.getPersonDetails()).ifPresent(person -> {
                    buildPThirdPartyPersonDetails(thirdPartyBuilder, person);
                    thirdPartyArrayBuilder.add(thirdPartyBuilder);
                });
                if (nonNull(courtApplicationParty.getOrganisationPersons())) {
                    courtApplicationParty.getOrganisationPersons().forEach(associatedPerson -> {
                        final JsonObjectBuilder associatedPersonBuilder = createObjectBuilder();
                        ofNullable(associatedPerson.getRole()).ifPresent(role -> associatedPersonBuilder.add(ROLE, role));
                        associatedPersonBuilder.add("name", associatedPerson.getPerson().getLastName());
                        thirdPartyBuilder.add(ORGANISATION, associatedPersonBuilder.build());
                        thirdPartyArrayBuilder.add(thirdPartyBuilder);
                    });
                }

                ofNullable(courtApplicationParty.getOrganisation()).ifPresent(organisation -> {
                    final JsonObjectBuilder associatedPersonBuilder = createObjectBuilder();
                    associatedPersonBuilder.add(NAME, organisation.getName());
                    thirdPartyBuilder.add(ORGANISATION, associatedPersonBuilder.build());
                    thirdPartyArrayBuilder.add(thirdPartyBuilder);
                });
            });

            courtApplicationBuilder.add("thirdParties", thirdPartyArrayBuilder);
        }

        ofNullable(courtApplication.getOutOfTimeReasons()).ifPresent(outOfTimeReason -> courtApplicationBuilder.add(OUT_OF_TIME_REASON, outOfTimeReason));


        final JsonObjectBuilder notificationBuilder = createObjectBuilder();
        notificationBuilder.add(COURT_APPLICATION, courtApplicationBuilder.build());
        if (nonNull(boxHearing)) {
            final CourtCentre courtCentre = boxHearing.getCourtCentre();
            final JsonObjectBuilder courtHearingCentreBuilder = createObjectBuilder();
            courtHearingCentreBuilder.add(NAME, courtCentre.getName());
            ofNullable(courtCentre.getCode()).ifPresent(name -> courtHearingCentreBuilder.add(NAME, name));
            final JsonObjectBuilder boxHearingRequestBuilder = createObjectBuilder();
            boxHearingRequestBuilder
                    .add(JURISDICTION_TYPE, boxHearing.getJurisdictionType().name())
                    .add(APPLICATION_DUE_DATE, boxHearing.getApplicationDueDate());
            ofNullable(boxHearing.getSendAppointmentLetter()).ifPresent(appointmentLetter -> boxHearingRequestBuilder.add(SEND_APPOINTMENT_LETTER, appointmentLetter));
            boxHearingRequestBuilder.add(COURT_CENTRE, courtHearingCentreBuilder.build());
            notificationBuilder.add(BOX_HEARING_REQUEST, boxHearingRequestBuilder.build());
        }

        return notificationBuilder.build();
    }

    private void buildApplicantIfApplicantIsOrganisation(final JsonObjectBuilder applicantBuilder, final CourtApplicationParty applicant) {
        final Optional<Organisation> organisation = ofNullable(applicant.getOrganisation());
        organisation.ifPresent(org -> {
            final JsonObjectBuilder nameBuilder = createObjectBuilder();
            nameBuilder.add(NAME, org.getName());
            applicantBuilder.add(ORGANISATION, nameBuilder.build());
        });

        ofNullable(applicant.getProsecutingAuthority()).map(ProsecutingAuthority::getName).ifPresent(name -> {
            final JsonObjectBuilder nameBuilder = createObjectBuilder();
            nameBuilder.add(NAME, name);
            applicantBuilder.add(ORGANISATION, nameBuilder.build());
        });

        final List<AssociatedPerson> organisationPersons = applicant.getOrganisationPersons();
        if (nonNull(organisationPersons)) {
            organisationPersons.forEach(associatedPerson -> {
                final JsonObjectBuilder nameBuilder = createObjectBuilder();
                ofNullable(associatedPerson.getRole()).ifPresent(role -> nameBuilder.add(ROLE, role));
                final Person person = associatedPerson.getPerson();
                if (nonNull(person)) {
                    buildPThirdPartyPersonDetails(nameBuilder, person);

                }
                applicantBuilder.add(ORGANISATION_PERSONS, nameBuilder);
            });
        }

        ofNullable(applicant.getPersonDetails()).ifPresent(person -> buildPThirdPartyPersonDetails(applicantBuilder, person));
    }

    private void buildApplicantWhenApplicantIsADefendant(final List<UUID> cpsProsecutorsList, final CourtApplication courtApplication, final List<ProsecutionCase> prosecutionCases, final JsonObjectBuilder applicantBuilder, final CourtApplicationParty applicant) {
        ofNullable(applicant.getMasterDefendant()).ifPresent(masterDefendant -> {
            Optional<Defendant> defendant = prosecutionCases.stream().flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                    .filter(defend -> ofNullable(masterDefendant.getMasterDefendantId()).isPresent() && defend.getMasterDefendantId().equals(ofNullable(masterDefendant.getMasterDefendantId()).get()))
                    .findFirst();
            defendant.ifPresent(defnDant -> {
                ofNullable(courtApplication.getDefendantASN()).ifPresent(asn -> applicantBuilder.add(ASN, asn));
                ofNullable(applicant.getOrganisation()).ifPresent(organisation -> {
                    final JsonObjectBuilder organisationBuilder = createObjectBuilder();
                    organisationBuilder.add(NAME, organisation.getName());
                    applicantBuilder.add(ORGANISATION, organisationBuilder.build());
                });
                final JsonArrayBuilder orgPersonArray = createArrayBuilder();
                final JsonObjectBuilder orgPerson = createObjectBuilder();
                final List<AssociatedPerson> organisationPersons = applicant.getOrganisationPersons();
                buildOrganizationPersonIfExistsToApplicant(applicantBuilder, orgPersonArray, orgPerson, organisationPersons);
                final PersonDefendant personDefendant = defnDant.getPersonDefendant();
                if (nonNull(personDefendant)) {
                    buildPersonDetails(applicantBuilder, personDefendant.getPersonDetails());
                } else if (isPartyCPS(applicant, cpsProsecutorsList)) {
                    applicantBuilder.add(ORGANISATION, createObjectBuilder().add(NAME, "CPS").build());
                }
                buildAssociatedPersonIfExistsToApplicant(applicantBuilder, defnDant);
            });
            ofNullable(masterDefendant.getCpsDefendantId()).ifPresent(cosDefendantId -> applicantBuilder.add(CSP_DEFENDANT_ID, cosDefendantId.toString()));

        });
    }


    private void buildAssociatedPersonIfExistsToApplicant(final JsonObjectBuilder applicantBuilder, final Defendant defnDant) {
        final List<AssociatedPerson> associatedPersonList = defnDant.getAssociatedPersons();
        if (nonNull(associatedPersonList)) {
            final JsonObjectBuilder associatedPersonBuilder = createObjectBuilder();
            final JsonArrayBuilder personArray = createArrayBuilder();
            associatedPersonList.forEach(associatedPerson -> {
                ofNullable(associatedPerson.getRole()).ifPresent(role -> associatedPersonBuilder.add(ROLE, role));
                personArray.add(associatedPersonBuilder.build());
            });
            applicantBuilder.add(ASSOCIATED_PERSONS, personArray.build());
        }
    }

    private void buildOrganizationPersonIfExistsToApplicant(final JsonObjectBuilder applicantBuilder, final JsonArrayBuilder orgPersonArray, final JsonObjectBuilder orgPerson, final List<AssociatedPerson> organisationPersons) {
        if (nonNull(organisationPersons)) {
            organisationPersons.forEach(organisationPerson -> {
                final JsonObjectBuilder personBuilder = createObjectBuilder();
                final Person person = organisationPerson.getPerson();
                if (nonNull(person)) {
                    buildPersonDetails(personBuilder, person);
                    orgPerson.add(PERSON, personBuilder.build());
                    orgPersonArray.add(orgPerson);
                }
            });
            applicantBuilder.add(ORGANISATION_PERSONS, orgPersonArray.build());
        }
    }

    private void buildCourtApplicationTypeAndPayment(final CourtApplication courtApplication, final JsonObjectBuilder courtApplicationBuilder, final JsonObjectBuilder courtApplicationPaymentBuilder) {
        ofNullable(courtApplication.getCourtApplicationPayment()).ifPresent(s -> {
            ofNullable(s.getPaymentReference()).ifPresent(payRef -> courtApplicationPaymentBuilder.add(PAYMENT_REFERENCE, payRef));
            courtApplicationBuilder
                    .add(COURT_APPLICATION_PAYMENT, courtApplicationPaymentBuilder.build());
        });
    }

    private void buildCourtApplicationAndType(final CourtApplication courtApplication, final Optional<String> code, final JsonObjectBuilder courtApplicationBuilder, final JsonObjectBuilder applicationTypeBuiler) {
        courtApplicationBuilder.add(APPLICATION_ID, courtApplication.getId().toString());
        courtApplicationBuilder.add(COURT_APPLICATION_CASES, buildCourtApplicationCasesArray(courtApplication.getCourtApplicationCases()).build());
        code.ifPresent(codeString -> applicationTypeBuiler.add(CODE, codeString));
        applicationTypeBuiler.add(CATEGORY_CODE, courtApplication.getType().getCategoryCode());
        applicationTypeBuiler.add(JURISDICTION, courtApplication.getType().getJurisdiction().toString());
        courtApplicationBuilder.add(COURT_APPLICATION_TYPE, applicationTypeBuiler);
    }

    private void buildPThirdPartyPersonDetails(final JsonObjectBuilder personBuilder, final Person person) {
        LOGGER.info("Person details to build");
        final JsonObjectBuilder personObjectBuilder = createObjectBuilder();
        ofNullable(person.getTitle()).ifPresent(title -> personObjectBuilder.add(TITLE, title));
        ofNullable(person.getFirstName()).ifPresent(firstName -> personObjectBuilder.add(FIRST_NAME, firstName));
        ofNullable(person.getMiddleName()).ifPresent(middleName -> personObjectBuilder.add(MIDDLE_NAME, middleName));
        personObjectBuilder.add(LAST_NAME, person.getLastName());
        ofNullable(person.getDateOfBirth()).ifPresent(dob -> personObjectBuilder.add(DATE_OF_BIRTH, dob));
        personBuilder.add(PERSON_DETAILS, personObjectBuilder);
    }

    private void buildPersonDetails(final JsonObjectBuilder personBuilder, final Person person) {
        LOGGER.info("Person details to build");
        ofNullable(person.getTitle()).ifPresent(title -> personBuilder.add(TITLE, title));
        ofNullable(person.getFirstName()).ifPresent(firstName -> personBuilder.add(FIRST_NAME, firstName));
        ofNullable(person.getMiddleName()).ifPresent(middleName -> personBuilder.add(MIDDLE_NAME, middleName));
        personBuilder.add(LAST_NAME, person.getLastName());
        ofNullable(person.getDateOfBirth()).ifPresent(dob -> personBuilder.add(DATE_OF_BIRTH, dob));
        ofNullable(person.getAddress()).ifPresent(address -> buildAddress(personBuilder, address));
    }


    private JsonArrayBuilder getRespondents(final CourtApplicationWithCaseDto courtApplicationDto, final List<UUID> cpsProsecutorsList) {
        final JsonArrayBuilder jsoArrayBuilder = createArrayBuilder();

        final CourtApplication courtApplication = courtApplicationDto.getCourtApplication();

        final boolean isSubjectMasterDefendant = ofNullable(courtApplication.getSubject().getMasterDefendant()).isPresent();

        final JsonObjectBuilder respondentBuilder = createObjectBuilder();

        courtApplication.getRespondents().forEach(respondent -> {
            ofNullable(respondent.getMasterDefendant()).ifPresent(masterDefendant -> {
                Optional<Defendant> defendant = courtApplicationDto.getProsecutionCases().stream()
                        .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                        .filter(defend ->
                                ofNullable(masterDefendant.getMasterDefendantId()).isPresent() && defend.getMasterDefendantId().equals(ofNullable(masterDefendant.getMasterDefendantId()).get()))
                        .findFirst();
                defendant.ifPresent(defnDant -> {
                    final JsonObjectBuilder associatedPersonBuilder = createObjectBuilder();
                    ofNullable(courtApplication.getDefendantASN()).ifPresent(asn -> respondentBuilder.add(ASN, asn));
                    if (nonNull(defnDant.getAssociatedPersons())) {
                        final JsonArrayBuilder personArray = createArrayBuilder();
                        defnDant.getAssociatedPersons().forEach(associatedPerson -> {
                            ofNullable(associatedPerson.getRole()).ifPresent(role -> associatedPersonBuilder.add(ROLE, role));
                            personArray.add(associatedPersonBuilder.build());
                        });
                        respondentBuilder.add(ASSOCIATED_PERSONS, personArray.build());
                    }

                    final PersonDefendant personDefendant = defnDant.getPersonDefendant();
                    if (nonNull(personDefendant)) {
                        buildPersonDetails(respondentBuilder, personDefendant.getPersonDetails());
                        respondentBuilder.add(IS_SUBJECT, false);
                    }
                });
                ofNullable(masterDefendant.getCpsDefendantId()).ifPresent(cosDefendantId -> respondentBuilder.add(CSP_DEFENDANT_ID, cosDefendantId.toString()));
            });

            respondentBuilder.add(SUMMONS_REQUIRED, respondent.getSummonsRequired());
            respondentBuilder.add(NOTIFICATION_REQUIRED, respondent.getNotificationRequired());

            ofNullable(respondent.getAppointmentNotificationRequired()).ifPresent(appointmentNotificationRequired ->
                    respondentBuilder.add(APPOINTMENT_NOTIFICATION_REQUIRED, appointmentNotificationRequired));

            final Optional<Organisation> organisation = ofNullable(respondent.getOrganisation());
            final boolean isRespondentHasMasterDefendant = ofNullable(respondent.getMasterDefendant()).isPresent();

            final JsonObjectBuilder orgBuilder = createObjectBuilder();

            organisation.ifPresent(o -> buildOrganisationToRespondentBuilder(respondentBuilder, orgBuilder, o));

            ofNullable(respondent.getPersonDetails()).ifPresent(personDetails -> {
                final JsonObjectBuilder peronDetailsBuilder = createObjectBuilder();
                buildPersonDetails(peronDetailsBuilder, personDetails);
                respondentBuilder.add(PERSON_DETAILS, peronDetailsBuilder.build());
            });

            if (isPartyCPS(respondent, cpsProsecutorsList)) {
                respondentBuilder.add(ORGANISATION, createObjectBuilder().add(NAME, "CPS").build());
            } else {
                ofNullable(respondent.getProsecutingAuthority()).ifPresent(prosecutingAuthority -> ofNullable(prosecutingAuthority.getName()).ifPresent(name -> respondentBuilder.add(ORGANISATION, createObjectBuilder().add("name", ofNullable(prosecutingAuthority.getName()).get()).build())));
            }

            final boolean isSubject = ofNullable(courtApplication.getSubject().getProsecutingAuthority()).isPresent();

            if (isSubject) {
                ofNullable(courtApplication.getSubject().getProsecutingAuthority()).ifPresent(prosecutingAuthority -> {
                    if (ofNullable(respondent.getProsecutingAuthority()).isPresent()) {
                        final ProsecutingAuthority applicant = ofNullable(respondent.getProsecutingAuthority()).get();
                        respondentBuilder.add(IS_SUBJECT, applicant.getProsecutionAuthorityId().equals(prosecutingAuthority.getProsecutionAuthorityId()));
                    } else {
                        respondentBuilder.add(IS_SUBJECT, false);
                    }
                });
            } else if (isSubjectMasterDefendant && isRespondentHasMasterDefendant) {
                ofNullable(respondent.getMasterDefendant()).ifPresent(masterDefendant -> ofNullable(courtApplication.getSubject().getMasterDefendant()).ifPresent(subjectMasterDefendant -> respondentBuilder.add(IS_SUBJECT, subjectMasterDefendant.getMasterDefendantId().equals(masterDefendant.getMasterDefendantId()))));
            } else {
                respondentBuilder.add(IS_SUBJECT, false);
            }
            jsoArrayBuilder.add(respondentBuilder.build());
        });
        return jsoArrayBuilder;
    }

    private boolean isPartyCPS(final CourtApplicationParty courtApplicationParty, final List<UUID> cpsProsecutorsList) {
        final Optional<ProsecutingAuthority> prosecutingAuthority = ofNullable(courtApplicationParty.getProsecutingAuthority());
        return prosecutingAuthority.filter(authority -> cpsProsecutorsList.contains(authority.getProsecutionAuthorityId())).isPresent();
    }

    private void buildOrganisationToRespondentBuilder(final JsonObjectBuilder respondentBuilder, final JsonObjectBuilder orgBuilder, final Organisation o) {
        orgBuilder.add(NAME, o.getName());
        ofNullable(o.getContact()).ifPresent(contactNumber -> buildContact(orgBuilder, contactNumber));
        ofNullable(o.getAddress()).ifPresent(address -> buildAddress(orgBuilder, address));
        ofNullable(o.getRegisteredCharityNumber()).ifPresent(charityNum -> orgBuilder.add(REGISTERED_CHARITY_NUMBER, charityNum));
        ofNullable(o.getIncorporationNumber()).ifPresent(incNumber -> orgBuilder.add(INCORPORATION_NUMBER, incNumber));
        respondentBuilder.add(ORGANISATION, orgBuilder.build());
    }

    private void buildContact(final JsonObjectBuilder contactObjectBuilder, final ContactNumber contactNumber) {
        final JsonObjectBuilder contactBuilder = createObjectBuilder();
        ofNullable(contactNumber.getPrimaryEmail()).ifPresent(primaryEmail -> contactBuilder.add(PRIMARY_EMAIL, primaryEmail));
        ofNullable(contactNumber.getSecondaryEmail()).ifPresent(secondaryEmail -> contactBuilder.add(SECONDARY_EMAIL, secondaryEmail));
        ofNullable(contactNumber.getFax()).ifPresent(fax -> contactBuilder.add(FAX, fax));
        ofNullable(contactNumber.getHome()).ifPresent(home -> contactBuilder.add(HOME, home));
        ofNullable(contactNumber.getMobile()).ifPresent(mobile -> contactBuilder.add(MOBILE, mobile));
        ofNullable(contactNumber.getWork()).ifPresent(work -> contactBuilder.add(WORK, work));
        contactObjectBuilder.add(CONTACT_NUMBER, contactBuilder.build());
    }

    private void buildAddress(final JsonObjectBuilder addressBuilder, final Address address) {
        final JsonObjectBuilder addressSubBuilder = createObjectBuilder();
        addressSubBuilder.add(ADDRESS_1, address.getAddress1());
        ofNullable(address.getAddress2()).ifPresent(address2 -> addressSubBuilder.add(ADDRESS_2, address2));
        ofNullable(address.getAddress3()).ifPresent(address3 -> addressSubBuilder.add(ADDRESS_3, address3));
        ofNullable(address.getAddress4()).ifPresent(address4 -> addressSubBuilder.add(ADDRESS_4, address4));
        ofNullable(address.getAddress5()).ifPresent(address5 -> addressSubBuilder.add(ADDRESS_5, address5));
        ofNullable(address.getPostcode()).ifPresent(postCode -> addressSubBuilder.add(POST_CODE, postCode));
        addressBuilder.add(ADDRESS, addressSubBuilder.build());
    }

    private JsonArrayBuilder buildCourtApplicationCasesArray(final List<CourtApplicationCase> courtApplicationCases) {
        final JsonArrayBuilder jsoArrayBuilder = createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        courtApplicationCases.forEach(courtApplicationCase -> {
            final Optional<String> ouCode = ofNullable(courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityOUCode());
            jsonObjectBuilder.add(CASE_URN, courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN());
            ouCode.ifPresent(s -> jsonObjectBuilder.add(PROSECUTOR_OU_CODE, ouCode.get()));
            jsoArrayBuilder.add(jsonObjectBuilder.build());
        });
        return jsoArrayBuilder;
    }
}