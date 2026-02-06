package uk.gov.moj.cpp.staging.prosecutors;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonArray;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static javax.json.JsonValue.ValueType.NUMBER;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.moj.cpp.staging.prosecutors.service.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.common.Title;

import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.lang3.EnumUtils;

@ServiceComponent(COMMAND_API)
public class StagingProsecutorsCommandAPIV3 {

    @Inject
    @Value(key = "stagingprosecutors.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    @Inject
    private Sender sender;

    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    private SystemIdMapperService systemIdMapperService;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";
    private static final String PROSECUTION_CASE_SUBJECT = "prosecutionCaseSubject";
    private static final String COURT_APPLICATION_SUBJECT = "courtApplicationSubject";
    private static final String EXHIBIT = "exhibit";
    private static final String WITNESS_STATEMENT = "witnessStatement";
    private static final String TAG = "tag";
    private static final String MATERIAL = "material";
    private static final String MATERIAL_TYPE = "materialType";
    private static final String MATERIAL_NAME = "materialName";
    private static final String MATERIAL_CONTENT_TYPE = "materialContentType";
    private static final String OU_CODE = "ouCode";
    private static final String PROSECUTOR_PERSON_DEFENDANT_DETAILS = "prosecutorPersonDefendantDetails";
    private static final String CPS_PERSON_DEFENDANT_DETAILS = "cpsPersonDefendantDetails";
    private static final String FILE_NAME = "fileName";
    private static final String SECTION_ORDER_SEQUENCE = "sectionOrderSequence";
    private static final String CASE_SUBFOLDER_NAME = "caseSubFolderName";
    private static final String STATEMENT_NUMBER = "statementNumber";
    private static final String STATEMENT_DATE = "statementDate";
    private static final String TITLE = "title";
    private static final String TITLE_SCHEMA_VIOLATION = "Error submitting material, request has schema violations [prosecutionCaseSubject/defendantSubject/%s/title: %s is not a valid enum value]";
    private static final String TAG_SCHEMA_VIOLATION = "Error submitting material, request has schema violations [tag/isSpltMergeTag : isSpltMergeTag should not be provided along with code]";
    private static final String SUBJECT_VIOLATION = "Error submitting material, request has schema violations, either [prosecutionCaseSubject] or [CourtApplicationSubject] is mandatory]";

    @Handles("stagingprosecutors.submit-material-v3")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {

        JsonObject requestPayload = envelope.payloadAsJsonObject();

        final Object prosecutionObject = requestPayload.get(PROSECUTION_CASE_SUBJECT);
        final Object courtApplicationObject = requestPayload.get(COURT_APPLICATION_SUBJECT);

        if (isNull(prosecutionObject) && isNull(courtApplicationObject)) {
            throw new BadRequestException(SUBJECT_VIOLATION);
        }

        if ((nonNull(prosecutionObject) && !(prosecutionObject instanceof JsonObject)) ||
                (nonNull(courtApplicationObject) && !(courtApplicationObject instanceof JsonObject))) {

            requestPayload = createPayload(requestPayload);
        } else {
            requestPayload = addOuCodeToPayload(requestPayload);
        }

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final Optional<JsonObject> prosecutionCaseSubject = getJsonObject(requestPayload, PROSECUTION_CASE_SUBJECT);
        if (prosecutionCaseSubject.isPresent()) {
            validateTitle(prosecutionCaseSubject.get());
        }

        final Optional<JsonArray> tagArray = getJsonArray(requestPayload, "tag");
        if (tagArray.isPresent()) {
            validateTag(tagArray.get());
        }

        final UUID submissionId = uuidProducer.generateUUID();
        requestPayload = createObjectBuilder(requestPayload).add("submissionId", submissionId.toString()).build();

        sender.send(envelop(requestPayload)
                .withName("stagingprosecutors.command.submit-material-v3")
                .withMetadataFrom(envelope));

        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }

    private JsonObject createPayload(final JsonObject requestPayload) {

        final JsonObjectBuilder builder = createObjectBuilder();
        createPayloadWithMandatoryProperties(requestPayload, builder);
        createPayloadWithOptionalProperties(requestPayload, builder);

        final Optional<String> prosecutionCaseSubject = getString(requestPayload, PROSECUTION_CASE_SUBJECT);
        if (prosecutionCaseSubject.isPresent()) {
            final JsonObject prosecutionCaseSubjectPayload = stringToJsonObjectConverter.convert(prosecutionCaseSubject.get());
            builder.add(PROSECUTION_CASE_SUBJECT, createObjectBuilder(prosecutionCaseSubjectPayload).add(OU_CODE, requestPayload.getString(OU_CODE)).build());
        }

        final Optional<String> courtApplicationSubject = getString(requestPayload, COURT_APPLICATION_SUBJECT);
        if (courtApplicationSubject.isPresent()) {
            final JsonObject courtApplicationSubjectPayload = stringToJsonObjectConverter.convert(courtApplicationSubject.get());
            builder.add(COURT_APPLICATION_SUBJECT, courtApplicationSubjectPayload);
        }

        return builder.build();
    }

    private void createPayloadWithMandatoryProperties(final JsonObject requestPayload, final JsonObjectBuilder builder) {
        final Optional<String> material = getString(requestPayload, MATERIAL);
        if (material.isPresent()) {
            builder.add(MATERIAL, requestPayload.getString(MATERIAL));
        }
        final Optional<String> materialType = getString(requestPayload, MATERIAL_TYPE);
        if (materialType.isPresent()) {
            builder.add(MATERIAL_TYPE, requestPayload.getString(MATERIAL_TYPE));
        }
        final Optional<String> materialName = getString(requestPayload, MATERIAL_NAME);
        if (materialName.isPresent()) {
            builder.add(MATERIAL_NAME, requestPayload.getString(MATERIAL_NAME));
        }
        final Optional<String> materialContentType = getString(requestPayload, MATERIAL_CONTENT_TYPE);
        if (materialContentType.isPresent()) {
            builder.add(MATERIAL_CONTENT_TYPE, requestPayload.getString(MATERIAL_CONTENT_TYPE));
        }
    }

    private void createPayloadWithOptionalProperties(final JsonObject requestPayload, final JsonObjectBuilder builder) {

        final Optional<String> fileName = getString(requestPayload, FILE_NAME);
        if (fileName.isPresent()) {
            builder.add(FILE_NAME, fileName.get());
        }
        if (nonNull(requestPayload.get(SECTION_ORDER_SEQUENCE)) && NUMBER.equals(requestPayload.get(SECTION_ORDER_SEQUENCE).getValueType())) {
            builder.add(SECTION_ORDER_SEQUENCE, requestPayload.get(SECTION_ORDER_SEQUENCE));
        } else {
            final Optional<String> sectionOrderSequence = getString(requestPayload, SECTION_ORDER_SEQUENCE);
            if (sectionOrderSequence.isPresent()) {
                try {
                    builder.add(SECTION_ORDER_SEQUENCE, Integer.parseInt(sectionOrderSequence.get()));
                } catch (NumberFormatException e) {
                    builder.add(SECTION_ORDER_SEQUENCE, sectionOrderSequence.get());
                }
            }
        }
        final Optional<String> caseSubFolderName = getString(requestPayload, CASE_SUBFOLDER_NAME);
        if (caseSubFolderName.isPresent()) {
            builder.add(CASE_SUBFOLDER_NAME, caseSubFolderName.get());
        }
        final Optional<String> exhibit = getString(requestPayload, EXHIBIT);
        if (exhibit.isPresent()) {
            builder.add(EXHIBIT, stringToJsonObjectConverter.convert(exhibit.get()));
        }
        final Optional<String> witnessStatement = getString(requestPayload, WITNESS_STATEMENT);
        if (witnessStatement.isPresent()) {
            createWitnessStatement(witnessStatement.get(), builder);
        }
        final Optional<String> tag = getString(requestPayload, TAG);
        if (tag.isPresent()) {
            createTags(tag.get(), builder);
        }
    }

    private void createWitnessStatement(final String witnessStatement, final JsonObjectBuilder builder) {
        final JsonObject witness = stringToJsonObjectConverter.convert(witnessStatement);
        final JsonObjectBuilder witnessBuilder = createObjectBuilder();
        if (nonNull(witness.get(STATEMENT_NUMBER)) && NUMBER.equals(witness.get(STATEMENT_NUMBER).getValueType())) {
            witnessBuilder.add(STATEMENT_NUMBER, witness.get(STATEMENT_NUMBER));
        } else {
            final Optional<String> statementNumber = getString(witness, STATEMENT_NUMBER);
            if (statementNumber.isPresent()) {
                try {
                    witnessBuilder.add(STATEMENT_NUMBER, Integer.parseInt(statementNumber.get()));
                } catch (NumberFormatException e) {
                    witnessBuilder.add(STATEMENT_NUMBER, statementNumber.get());
                }
            }
        }
        final Optional<String> dateTaken = getString(witness, STATEMENT_DATE);

        if (dateTaken.isPresent()) {
            witnessBuilder.add(STATEMENT_DATE, dateTaken.get());
        }
        builder.add(WITNESS_STATEMENT, witnessBuilder.build());
    }

    private void createTags(final String tag, final JsonObjectBuilder builder) {
        final JsonReader jsonReader = createReader(new StringReader(tag));
        final JsonArray tagArray = jsonReader.readArray();
        jsonReader.close();
        builder.add(TAG, tagArray);
    }

    private void validateTitle(final JsonObject prosecutionCaseSubjectPayload) {

        final Optional<JsonObject> defendantSubject = getJsonObject(prosecutionCaseSubjectPayload, "defendantSubject");

        if (defendantSubject.isPresent()) {
            final Optional<JsonObject> prosecutorPersonDefendantDetails = getJsonObject(defendantSubject.get(), PROSECUTOR_PERSON_DEFENDANT_DETAILS);
            if (prosecutorPersonDefendantDetails.isPresent()) {
                final Optional<String> title = getString(prosecutorPersonDefendantDetails.get(), TITLE);
                if (title.isPresent() && !EnumUtils.isValidEnum(Title.class, title.get())) {
                    throw new BadRequestException(format(TITLE_SCHEMA_VIOLATION, PROSECUTOR_PERSON_DEFENDANT_DETAILS, title.get()));
                }
            }
            final Optional<JsonObject> cpsPersonDefendantDetails = getJsonObject(defendantSubject.get(), CPS_PERSON_DEFENDANT_DETAILS);
            if (cpsPersonDefendantDetails.isPresent()) {
                final Optional<String> title = getString(cpsPersonDefendantDetails.get(), TITLE);
                if (title.isPresent() && !EnumUtils.isValidEnum(Title.class, title.get())) {
                    throw new BadRequestException(format(TITLE_SCHEMA_VIOLATION, CPS_PERSON_DEFENDANT_DETAILS, title.get()));
                }
            }
        }
    }

    private void validateTag(JsonArray tags) {
        tags.getValuesAs(JsonObject.class).stream().forEach(tag -> {
            final Optional<String> code = getString(tag, "code");
            final Optional<Boolean> isSplitMerge = getBoolean(tag, "isSpltMergeTag");
            if (code.isPresent() && isSplitMerge.isPresent()) {
                throw new BadRequestException(TAG_SCHEMA_VIOLATION);
            }
        });
    }

    private JsonObject addOuCodeToPayload(JsonObject requestPayload){
        final JsonObjectBuilder builder = createObjectBuilder();
        final Optional<JsonObject> prosecutionCaseSubject =  getJsonObject(requestPayload, PROSECUTION_CASE_SUBJECT);
        if(prosecutionCaseSubject.isPresent()){
            requestPayload.forEach(builder::add);
            builder.add(PROSECUTION_CASE_SUBJECT, createObjectBuilder(prosecutionCaseSubject.get()).add(OU_CODE, requestPayload.getString(OU_CODE)).build());
            return builder.build();
        }else{
            return requestPayload;
        }
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }
}
