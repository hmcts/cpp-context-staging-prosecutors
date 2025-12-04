package uk.gov.moj.cpp.staging.prosecutors.validators;

import static java.lang.String.format;

public abstract class SubmitSjpProsecutionTextResources {

    public static final String GENDER_NOT_ACCEPTABLE = "Gender of the defendant should have one of the values 0, 1, 2 or 9";

    public static final String OFFENCE_DATE_CODE_NOT_ACCEPTABLE = "Offence date code of the offences should have one of the values 1, 2, 3, 4, 5 or 6";

    public static final String OFFENCE_DATE_INVALID_DURATION = "Cannot enter empty or invalid offence committed date and offence committed end date when offence date code is 4";

    public static final String OFFENCE_DATE_INVALID_DATE = "Cannot enter empty or invalid offence committed date when offence date code is 1, 2, 3, 5 or 6";

    public static final String HOME_NUMBER_INVALID = "Cannot enter an invalid home phone number";

    public static final String WORK_NUMBER_INVALID = "Cannot enter an invalid work phone number";

    public static final String MOBILE_NUMBER_INVALID = "Cannot enter an invalid mobile phone number";

    public static final String OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE = "The offences %s have same offence sequence numbers. Offence sequence numbers must be unique.";

    public static final String DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE = "The defendants %s have same prosecutor defendant ids. Prosecutor defendant ids must be unique.";

    public static final String FIELD_MOBILE_NUMBER = "defendant.defendantPerson.contactDetails.mobileTelephoneNumber";

    public static final String FIELD_HOME_NUMBER = "defendant.defendantPerson.contactDetails.homeTelephoneNumber";

    public static final String FIELD_PROSECUTOR_DEFENDANT_ID = "defendant.defendantDetails.prosecutorDefendantId";

    public static final String FIELD_WORK_NUMBER = "defendant.defendantPerson.contactDetails.workTelephoneNumber";

    public static final String FIELD_GENDER = "defendant.defendantPerson.selfDefinedInformation.gender";

    public static final String FIELD_OFFENCE_DATE_CODE = "defendant.offences[0].offenceDateCode";

    public static final String FIELD_OFFENCE_OFFENCE_SEQUENCE_NO = "defendant.offences.offenceSequenceNo";

    private SubmitSjpProsecutionTextResources() {
        // prevent initialization
    }
}
