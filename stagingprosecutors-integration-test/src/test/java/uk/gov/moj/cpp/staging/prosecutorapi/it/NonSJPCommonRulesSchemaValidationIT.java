package uk.gov.moj.cpp.staging.prosecutorapi.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.FIELD_DEFENDANT_PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.FIELD_OFFENCE_OFFENCE_SEQUENCE_NO;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.BusinessValidationsTextResources.OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.CHARGE_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.OUCODE;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.REQUISITION_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.SUMMONS_CONTENT_TYPE_V2;
import static uk.gov.moj.cpp.staging.prosecutorapi.utils.StagingProsecutors.postCommandV2;

import uk.gov.moj.cpp.staging.prosecutorapi.utils.WiremockUtils;

import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NonSJPCommonRulesSchemaValidationIT {

    private static final String FIELD_VALIDATION_ERRORS = "validationErrors";
    private static final String FIELD_ERROR = "error";
    private static final String OFFENCE_CODES_1 = "[CA03010, CA03014]";
    private static final String OFFENCE_CODES_2 = "[CA03015, CA03015]";
    private static final String PROSECUTOR_DEFENDANT_IDS = "[PROSECUTORDEFENDANTID1, PROSECUTORDEFENDANTID1]";

    @BeforeEach
    public void setup() {
        new WiremockUtils();
    }

    @Test
    public void summonsSchemaValidationShouldFail() {
        final String response = postCommandV2("importCase/nonsjp/summons/stagingprosecutors.submit-summons-prosecution-schema-errors.json", SUMMONS_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final String validationTrace = getValidationTrace(response);

        assertErrors(validationTrace);
    }

    @Test
    public void summonsShouldFailWithOffenceSequenceNumberAndProsecutorDefendantIdNotUniqueErrors() {
        final String response = postCommandV2("importCase/nonsjp/summons/stagingprosecutors.submit-summons-prosecution-fail-fast-errors.json", SUMMONS_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final JsonArray offenceNotUniqueError = getSequenceNoNotUniqueError(response);
        final String prosecutorDefendantIdError = getProsecutorDefendantIdNotUniqueError(response);

        assertSequenceNumberNotUnique(offenceNotUniqueError);
        assertProsecutorDefendantIdNotUnique(prosecutorDefendantIdError);
    }

    @Test
    public void requisitionSchemaValidationShouldFail() {
        final String response = postCommandV2("importCase/nonsjp/requisition/stagingprosecutors.submit-requisition-prosecution-schema-errors.json", REQUISITION_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final String validationTrace = getValidationTrace(response);

        assertErrors(validationTrace);
    }

    @Test
    public void requisitionShouldFailWithOffenceSequenceNumberAndProsecutorDefendantIdNotUniqueErrors() {
        final String response = postCommandV2("importCase/nonsjp/requisition/stagingprosecutors.submit-requisition-prosecution-fail-fast-errors.json", REQUISITION_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final JsonArray offenceNotUniqueError = getSequenceNoNotUniqueError(response);
        final String prosecutorDefendantIdError = getProsecutorDefendantIdNotUniqueError(response);

        assertSequenceNumberNotUnique(offenceNotUniqueError);
        assertProsecutorDefendantIdNotUnique(prosecutorDefendantIdError);
    }

    @Test
    public void chargeSchemaValidationShouldFail() {
        final String response = postCommandV2("importCase/nonsjp/charge/stagingprosecutors.submit-charge-schema-errors.json", CHARGE_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final String validationTrace = getValidationTrace(response);

        assertErrors(validationTrace);
    }

    @Test
    public void chargeShouldFailWithOffenceSequenceNumberAndProsecutorDefendantIdNotUniqueErrors() {
        final String response = postCommandV2("importCase/nonsjp/charge/stagingprosecutors.submit-charge-fail-fast-errors.json", CHARGE_CONTENT_TYPE_V2, OUCODE, OUCODE).readEntity(String.class);

        final JsonArray offenceNotUniqueError = getSequenceNoNotUniqueError(response);
        final String prosecutorDefendantIdError = getProsecutorDefendantIdNotUniqueError(response);

        assertSequenceNumberNotUnique(offenceNotUniqueError);
        assertProsecutorDefendantIdNotUnique(prosecutorDefendantIdError);
    }

    private String getValidationTrace(final String response) {
        final JsonObject responseJson = stringToJsonObject(response);
        return responseJson.get(FIELD_VALIDATION_ERRORS).toString();
    }

    private JsonArray getSequenceNoNotUniqueError(final String response) {
        final JsonObject responseJson = stringToJsonObject(response);
        final JsonObject errors = stringToJsonObject(responseJson.getString(FIELD_ERROR));
        return errors.getJsonArray(FIELD_OFFENCE_OFFENCE_SEQUENCE_NO);
    }

    private String getProsecutorDefendantIdNotUniqueError(final String response) {
        final JsonObject responseJson = stringToJsonObject(response);
        final JsonObject errors = stringToJsonObject(responseJson.getString(FIELD_ERROR));
        return errors.getJsonArray(FIELD_DEFENDANT_PROSECUTOR_DEFENDANT_ID).getString(0);
    }

    private void assertSequenceNumberNotUnique(final JsonArray errorMessage) {
        assertThat(errorMessage.getString(0), equalTo(format(OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE, OFFENCE_CODES_1)));
        assertThat(errorMessage.getString(1), equalTo(format(OFFENCE_OFFENCE_SEQUENCE_NO_MUST_BE_UNIQUE, OFFENCE_CODES_2)));
    }

    private void assertProsecutorDefendantIdNotUnique(final String errorMessage) {
        assertThat(errorMessage, equalTo(format(DEFENDANT_PROSECUTOR_DEFENDANT_ID_MUST_BE_UNIQUE, PROSECUTOR_DEFENDANT_IDS)));
    }

    private void assertErrors(final String validationTrace) {

        with(validationTrace)
                .assertEquals("$.message", "#/defendants: 26 schema violations found");

        assertThat(validationTrace, containsString("#/defendants/0/individual/gender: 6 is not a valid enum value"));

        assertThat(validationTrace, containsString("#/defendants/0/defendantDetails/address/postcode: string [PG0 1XXXX] does not match pattern ^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) [0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"));
        assertThat(validationTrace, containsString("#/defendants/0/defendantDetails/address/postcode: expected maxLength: 8, actual: 9"));
        assertThat(validationTrace, containsString("#/defendants/0/defendantDetails/pncIdentifier: string [2099A1234567] does not match pattern ^(?!0{4}|0{3}|0{2}}|0{1})([0-9][0-9]{3})([/])([0-9]{7})([a-zA-Z]|[0-9])$"));
        assertThat(validationTrace, containsString("#/defendants/0/defendantDetails/croNumber: string [123456A20L] does not match pattern ^(([S][F])(\\\\d{2})([/])([0-9]{6})([a-zA-Z]|[0-9])|(\\\\d{6})([/])(\\\\d{2})([a-zA-Z]|[0-9]{1}))$"));

        assertThat(validationTrace, containsString("#/defendants/0/individual/contactDetails/homeTelephoneNumber: string [abcdef] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/contactDetails/mobileTelephoneNumber: string [hekf@fgg.com] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/contactDetails/workTelephoneNumber: string [12312312302345a23] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/contactDetails/primaryEmail: string [notgoodprimaryemail] does not match pattern ^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/contactDetails/secondaryEmail: string [notgoodsecondaryemail] does not match pattern ^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"));

        assertThat(validationTrace, containsString("#/defendants/0/individual/nationalInsuranceNumber: expected maxLength: 9, actual: 11"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/nationalInsuranceNumber: string [SN123456123] does not match pattern ^(?!BG)(?!GB)(?!NK)(?!KN)(?!TN)(?!NT)(?!ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9][0-9][0-9][0-9][0-9][0-9][A-D ]$"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/nationalInsuranceNumber: expected minLength: 9, actual: 8"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/nationalInsuranceNumber: string [SN12345C] does not match pattern ^(?!BG)(?!GB)(?!NK)(?!KN)(?!TN)(?!NT)(?!ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9][0-9][0-9][0-9][0-9][0-9][A-D ]$"));

        assertThat(validationTrace, containsString("#/defendants/0/offences/0/offenceDetails/offenceDateCode: 7 is not a valid enum value"));
        assertThat(validationTrace, containsString("#/defendants/1/offences/0/offenceDetails/offenceDateCode: 0 is not a valid enum value"));

        assertThat(validationTrace, containsString("#/defendants/0/offences/0/offenceDetails/offenceSequenceNo: 0.0 is not greater or equal to 1"));

        assertThat(validationTrace, containsString("#/defendants/0/individual/parentGuardian/address/postcode: string [abcdef] does not match pattern ^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) [0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"));
        assertThat(validationTrace, containsString("#/defendants/0/individual/parentGuardian/organisation/companyTelephoneNumber: string [12312312302345a23] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));

        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/gender: 8 is not a valid enum value"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/contactDetails/homeTelephoneNumber: string [abcdef] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/contactDetails/mobileTelephoneNumber: string [hekf@fgg.com] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/contactDetails/workTelephoneNumber: string [12312312302345a23] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/contactDetails/primaryEmail: string [notgoodprimaryemail] does not match pattern ^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"));
        assertThat(validationTrace, containsString("#/defendants/1/individual/parentGuardian/individual/contactDetails/secondaryEmail: string [notgoodsecondaryemail] does not match pattern ^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"));

        assertThat(validationTrace, containsString("#/defendants/2/organisation/companyTelephoneNumber: string [abcdef] does not match pattern ^[0-9+\\\\ \\\\-]{10,}$"));

    }

    private JsonObject stringToJsonObject(String response) {
        try (StringReader reader = new StringReader(response)) {
            return JsonObjects.createReader(reader).readObject();
        }
    }


}
