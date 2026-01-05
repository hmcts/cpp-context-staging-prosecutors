package uk.gov.moj.cpp.staging.prosecutors.helper;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class StagingProsecutorsHelper {

    public static JsonObject givenPayload(final String filename) {
        return JsonObjects.createReader(ClassLoader.
                getSystemResourceAsStream(filename)).readObject();
    }

    public static JsonObject createProsecutionCaseSubject(final UUID defendantId, final String testString) {
        final JsonObject payload = createObjectBuilder()
                .add("caseUrn", testString)
                .add("prosecutingAuthority", testString)
                .add("defendantSubject", createObjectBuilder()
                        .add("prosecutorPersonDefendantDetails", createObjectBuilder()
                                .add("forename", testString)
                                .add("surname", testString)
                                .add("prosecutorDefendantId", defendantId.toString())
                                .add("dateOfBirth", "1985-02-03")
                                .add("title", "Mr")
                                .build())
                        .build())
                .build();
        return payload;
    }

    public static JsonObject createCpsProsecutionCaseSubject(final UUID defendantId, final String testString) {
        final JsonObject payload = createObjectBuilder()
                .add("caseUrn", testString)
                .add("prosecutingAuthority", testString)
                .add("defendantSubject", createObjectBuilder()
                        .add("cpsPersonDefendantDetails", createObjectBuilder()
                                .add("forename", testString)
                                .add("surname", testString)
                                .add("cpsDefendantId", defendantId.toString())
                                .add("dateOfBirth", "1985-02-03")
                                .add("title", "invalid")
                                .build())
                        .build())
                .build();
        return payload;
    }

    public static JsonObject createCourtApplicationSubject(final UUID courtApplicationId) {
        return createObjectBuilder()
                        .add("courtApplicationId", courtApplicationId.toString())
                        .build();

    }

    public static JsonObjectBuilder createExpectedPayloadWithProsecutionCaseSubject(final UUID material, final UUID defendantId, final String testString) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("material", material.toString())
                .add("materialType", "SJPN")
                .add("materialName", "defendant-material")
                .add("materialContentType", "image/jpeg")
                .add("fileName", "Material-File")
                .add("sectionOrderSequence", 1)
                .add("caseSubFolderName", "Defendant-Material")
                .add("prosecutionCaseSubject", createObjectBuilder()
                        .add("caseUrn", testString)
                        .add("prosecutingAuthority", testString)
                        .add("defendantSubject", createObjectBuilder()
                                .add("prosecutorPersonDefendantDetails", createObjectBuilder()
                                        .add("forename", testString)
                                        .add("surname", testString)
                                        .add("prosecutorDefendantId", defendantId.toString())
                                        .add("dateOfBirth", "1985-02-03")
                                        .add("title", "Mr")
                                        .build())
                                .build())
                        .add("ouCode", "ouCode")
                        .build());
        return builder;
    }

    public static JsonObjectBuilder createExpectedPayloadWithCourtApplicationSubject(final UUID material, final UUID courtApplicationId) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("material", material.toString())
                .add("materialType", "SJPN")
                .add("materialName", "defendant-material")
                .add("materialContentType", "image/jpeg")
                .add("fileName", "Material-File")
                .add("sectionOrderSequence", 1)
                .add("caseSubFolderName", "Defendant-Material")
                .add("courtApplicationSubject", createObjectBuilder()
                        .add("courtApplicationId", courtApplicationId.toString())
                        .build());
        return builder;
    }

    public static JsonObjectBuilder createActualPayload() {
        return createObjectBuilder()
                .add("materialType", "SJPN")
                .add("materialName", "defendant-material")
                .add("materialContentType", "image/jpeg")
                .add("fileName", "Material-File")
                .add("sectionOrderSequence", "1")
                .add("caseSubFolderName", "Defendant-Material");
    }

    public static JsonObject createWitnessStatement() {
        return createObjectBuilder()
                .add("statementNumber", "1")
                .add("statementDate", "2021-03-09T14:30:04.881Z")
                .build();
    }

    public static JsonArrayBuilder createTags() {
        final JsonObject tag1 = createObjectBuilder()
                .add("name", "material-tag")
                .build();

        final JsonObject tag2 = createObjectBuilder()
                .add("name", "material-tag")
                .build();

        return createArrayBuilder()
                .add(tag1)
                .add(tag2);
    }

    public static JsonObject createExhibit() {
        return createObjectBuilder()
                .add("reference", "material-reference")
                .build();
    }

    public static JsonObject createWitnessStatementExpected() {
        return createObjectBuilder()
                .add("statementNumber", 1)
                .add("statementDate", "2021-03-09T14:30:04.881Z")
                .build();
    }
}
