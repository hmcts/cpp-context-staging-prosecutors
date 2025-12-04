package uk.gov.moj.cpp.staging.prosecutorapi.utils;

public abstract class BusinessValidationsTextResources {

    public static final String GENDER_NOT_ACCEPTABLE = "Gender of the defendant should have one of the values 0, 1, 2 or 9";
    
    public static final String OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE = "The offences %s have same offence sequence numbers. Offence sequence numbers must be unique.";

    public static final String DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE = "The defendants %s have same prosecutor defendant ids. Prosecutor defendant ids must be unique.";

    public static final String FIELD_MOBILE_NUMBER = "defendant/defendantPerson/contactDetails/mobileTelephoneNumber";

    public static final String FIELD_HOME_NUMBER = "defendant/defendantPerson/contactDetails/homeTelephoneNumber";

    public static final String FIELD_WORK_NUMBER = "defendant/defendantPerson/contactDetails/workTelephoneNumber";

    public static final String FIELD_GENDER = "defendant.defendantPerson.selfDefinedInformation.gender";

    public static final String FIELD_OFFENCE_OFFENCE_SEQUENCE_NO = "defendant.offences.offenceSequenceNo";

    public static final String FIELD_DEFENDANT_PROSECUTOR_DEFENDANT_ID = "defendant.defendantDetails.prosecutorDefendantId";

    private BusinessValidationsTextResources() {
        // prevent initialization
    }

}
