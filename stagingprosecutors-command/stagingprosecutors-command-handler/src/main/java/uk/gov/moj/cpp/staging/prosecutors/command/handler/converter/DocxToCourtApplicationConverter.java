package uk.gov.moj.cpp.staging.prosecutors.command.handler.converter;


import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication.submitApplication;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.of;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Address.address;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Applicant.applicant;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.AssociatedPerson.associatedPerson;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.BoxHearingRequest.boxHearingRequest;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.ContactNumber.contactNumber;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationCase.courtApplicationCase;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtCentre.courtCentre;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Organisation.organisation;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Person.person;
import static uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Respondent.respondent;


import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.command.SubmitApplication;


import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.AssociatedPerson;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.ContactNumber;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationCase;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.JurisdictionType;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Person;
import uk.gov.moj.cpp.staging.prosecutors.application.json.schemas.Respondent;
import uk.gov.moj.cpp.staging.prosecutors.command.handler.util.StructuredData;

public class DocxToCourtApplicationConverter {

    public static final String RESPONDENT = "respondent";
    public static final String RESPONDENT_2 = "respondent2";
    public static final String RESPONDENT_3 = "respondent3";
    public static final String RESPONDENT_4 = "respondent4";
    public static final String ORGANISATION_NAME = "-organisation-name";
    public static final String LAST_NAME = "-last-name";

    private DocxToCourtApplicationConverter() {

    }

    public static SubmitApplication prepareSubmitApplication(final Map<String, String> structuredDataMap, final CourtApplicationType courtApplicationType, final LocalDate nextBusinessDay) {
        return submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(UUID.randomUUID())
                        .withCourtApplicationType(courtApplicationType)
                        .withCourtApplicationCases(getCourtApplicationCases(structuredDataMap))
                        .withApplicant(applicant()
                                .withIsSubject(false)
                                .withOrganisation(organisation()
                                        .withName(structuredDataMap.get("applicant-organisation-name"))
                                        .build())
                                .withOrganisationPersons(getApplicantOrganisationPersons(structuredDataMap))
                                .build())
                        .withRespondents(getRespondents(structuredDataMap))
                        .build())
                .withBoxHearingRequest(boxHearingRequest()
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withApplicationDueDate(nextBusinessDay)
                        .withCourtCentre(courtCentre()
                                .withName(structuredDataMap.get("court-name"))
                                .build())

                        .build())
                .build();
    }

    private static List<AssociatedPerson> getApplicantOrganisationPersons(Map<String, String> structuredDataMap) {
        return isNull(structuredDataMap.get("applicant-last-name")) ? null : of(associatedPerson()
                .withPerson(person()
                        .withFirstName(structuredDataMap.get("applicant-first-name"))
                        .withLastName(structuredDataMap.get("applicant-last-name"))
                        .withAddress(getAddress(structuredDataMap, "applicant"))
                        .withContact(getContact(structuredDataMap))
                        .withGender(Gender.NOT_KNOWN)
                        .build())
                .build()).collect(toList());
    }

    private static List<CourtApplicationCase> getCourtApplicationCases(Map<String, String> structuredDataMap) {
        return isNull(structuredDataMap.get("related-case-urn")) ? null : of(courtApplicationCase()
                .withCaseURN(structuredDataMap.get("related-case-urn"))
                .withProsecutorOuCode(structuredDataMap.get("related-case-prosecutor-ou-code"))
                .build())
                .collect(toList());
    }

    private static ContactNumber getContact(final Map<String, String> structuredDataMap) {
        if (isNull(structuredDataMap.get("applicant-contact-number")) && isNull(structuredDataMap.get("applicant-email-address"))) {
            return null;
        } else {
            return contactNumber()
                    .withWork(structuredDataMap.get("applicant-contact-number"))
                    .withPrimaryEmail(structuredDataMap.get("applicant-email-address"))
                    .build();
        }
    }

    private static List<Respondent> getRespondents(final Map<String, String> structuredDataMap) {
        final int count = getMatchedRespondents(structuredDataMap);
        return rangeClosed(1, count)
                .mapToObj(i -> RESPONDENT + (i == 1 ? "" : i))
                .map(res -> respondent()
                        .withAsn(structuredDataMap.get(res + "-asn"))
                        .withCpsDefendantId(structuredDataMap.get(res + "-cps-defendant-id"))
                        .withOrganisation(getRespondentOrganisation(structuredDataMap, res))
                        .withIsSubject(RESPONDENT.equals(res))
                        .withPersonDetails(getRespondentPersonDetails(structuredDataMap, res))
                        .build())
                .collect(collectingAndThen(toList(), list -> list.isEmpty() ? null : list));
    }

    private static int getMatchedRespondents(Map<String, String> structuredDataMap) {
        final Map<String, String> searchRespondent = new HashMap<>();
        searchRespondent.put(RESPONDENT, "respondent value");
        searchRespondent.put(RESPONDENT_2, "respondent1 value");
        searchRespondent.put(RESPONDENT_3, "respondent2 value");
        searchRespondent.put(RESPONDENT_4, "respondent3 value");

        final Set<String> uniqueRespondentKeys = new HashSet<>();
        for (final String key : structuredDataMap.keySet()) {
            for (final String searchKey : searchRespondent.keySet()) {
                if (key.startsWith(searchKey)) {
                    uniqueRespondentKeys.add(searchKey);
                }
            }
        }
        return uniqueRespondentKeys.size();
    }

    private static Person getRespondentPersonDetails(Map<String, String> structuredDataMap, String res) {
        return isNull(structuredDataMap.get(res + LAST_NAME)) ? null : person()
                .withFirstName(structuredDataMap.get(res + "-first-name"))
                .withLastName(structuredDataMap.get(res + LAST_NAME))
                .withAddress(getAddress(structuredDataMap, res))
                .withGender(Gender.NOT_KNOWN)
                .build();
    }

    private static Organisation getRespondentOrganisation(Map<String, String> structuredDataMap, String res) {
        return isNull(structuredDataMap.get(res + ORGANISATION_NAME)) ? null : organisation()
                .withName(structuredDataMap.get(res + ORGANISATION_NAME))
                .withAddress(getAddress(structuredDataMap, res))
                .build();
    }

    private static Address getAddress(final Map<String, String> structuredDataMap, final String res) {
        return isNull(structuredDataMap.get(res + "-address-line1")) ? null : address()
                .withAddress1(structuredDataMap.get(res + "-address-line1"))
                .withAddress2(structuredDataMap.get(res + "-address-line2"))
                .withAddress3(structuredDataMap.get(res + "-address-line3"))
                .withAddress4(structuredDataMap.get(res + "-address-line4"))
                .withAddress5(structuredDataMap.get(res + "-address-line5"))
                .withPostcode(structuredDataMap.get(res + "-address-postcode"))
                .build();
    }

    public static Map<String, String> parse(final InputStream fileStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(fileStream)) {
            return doc.getTables().stream()
                    .map(XWPFTable::getCTTbl)
                    .map(CTTbl::getTrList)
                    .flatMap(List::stream)
                    .map(CTRow::getTcList)
                    .flatMap(List::stream)
                    .map(CTTc::getPList)
                    .flatMap(List::stream)
                    .map(CTP::getSdtList)
                    .flatMap(List::stream)
                    .map(sdtRun -> new StructuredData(getTagName(sdtRun), getTagValue(sdtRun)))
                    .filter(std -> nonNull(std.getValue()))
                    .collect(Collectors.toMap(StructuredData::getKey, StructuredData::getValue));
        }
    }

    private static String getTagName(final CTSdtRun sdtRun) {
        final CTSdtPr sdtPr = sdtRun.getSdtPr();
        return sdtPr.getTag().getVal();
    }

    private static String getTagValue(final CTSdtRun sdtRun) {
        final String value = getValueFromSdtContent(sdtRun.getSdtContent());
        return value.matches(".*[a-zA-Z0-9].*") ? value : null;
    }


    private static String getValueFromSdtContent(final CTSdtContentRun sdtContent) {
        if (!sdtContent.getSdtList().isEmpty()) {
            final String value = getValueFromSdtContent(sdtContent.getSdtList().get(0).getSdtContent());
            return value + sdtContent.getRList().stream()
                    .map(CTR::getTList)
                    .flatMap(List::stream)
                    .map(CTText::getStringValue)
                    .collect(Collectors.joining());
        } else {
            return sdtContent.getRList().stream()
                    .map(CTR::getTList)
                    .flatMap(List::stream)
                    .map(CTText::getStringValue)
                    .collect(Collectors.joining());
        }
    }
}
