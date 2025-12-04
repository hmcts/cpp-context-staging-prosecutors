package uk.gov.moj.cpp.staging.prosecutors.event.processor;

import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationRequestNotification.applicationRequestNotification;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationStatus.ERROR;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ValidationError.DEFENDANT_ASN_NOT_FOUND;
import static uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ValidationError.DEFENDANT_DETAILS_NOT_FOUND;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful.receiveMaterialSubmissionSuccessful;

import uk.gov.justice.core.courts.ApplicationExternalCreatorType;
import uk.gov.justice.core.courts.CotrOperationFailed;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Error;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationRequestNotification;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.ApplicationStatus;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.CaseDefendantIndividual;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.application.CaseDefendantOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.dto.CourtApplicationWithCaseDto;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.util.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.event.processor.utils.RestEasyClientService;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveMaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.UpdateSubmissionStatus;
import uk.gov.moj.cps.progression.domain.event.CotrCreated;
import uk.gov.moj.cps.progression.domain.event.CotrReviewNotesUpdated;
import uk.gov.moj.cps.progression.domain.event.FormCreated;
import uk.gov.moj.cps.progression.domain.event.FormOperationFailed;
import uk.gov.moj.cps.progression.domain.event.PetFormCreated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import dto.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.CpsApiService;
import service.CpsPayloadTransformService;

@SuppressWarnings({"squid:S3655", "squid:S3776", "pmd:NullAssignment"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionPublicEventProcessor {

    static final String UPDATE_SUBMISSION_STATUS_COMMAND = "stagingprosecutors.command.update-submission-status";
    static final String SUBMISSION_ID_STR = "submissionId";
    private static final String FEATURE_DEFENCE_DISCLOSURE = "defenceDisclosure";

    private static final String COTR_ID_NOT_FOUND = "COTR_ID_NOT_FOUND";
    private static final String HEARING_ID_NOT_FOUND = "HEARING_ID_NOT_FOUND";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionPublicEventProcessor.class);

    @Inject
    private RestEasyClientService restEasyClientService;

    @Inject
    private CpsApiService cpsApiService;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CpsPayloadTransformService cpsPayloadTransformService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("public.progression.court-document-added")
    public void caseDocumentUploaded(final JsonEnvelope courtDocumentAdded) {
        final JsonObject metadataJson = courtDocumentAdded.metadata().asJsonObject();

        final Optional<UUID> submissionId = ofNullable(
                courtDocumentAdded.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null))
                .map(UUID::fromString);

        if (submissionId.isPresent()) {
            final ReceiveMaterialSubmissionSuccessful command = receiveMaterialSubmissionSuccessful()
                    .withSubmissionId(submissionId.get())
                    .build();

            final Metadata metadata = Envelope.metadataFrom(courtDocumentAdded.metadata()).withName("stagingprosecutors.command.receive-material-submission-successful").build();
            sender.send(envelopeFrom(
                    metadata,
                    command));
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Received CourtDocumentAdded event with no submissionId[Metadata: {}], [Payload: {}]",
                        metadataJson, courtDocumentAdded.toObfuscatedDebugString());
            }
        }
    }

    @Handles("public.progression.form-operation-failed")
    public void formOperationFailed(final Envelope<FormOperationFailed> formOperationFailed) {
        LOGGER.info("public.progression.form-operation-failed event ...");

        final Optional<UUID> submissionId = ofNullable(
                formOperationFailed.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info("Submission ID not found. Form rejected event ignored");
            return;
        }

        updateSucessSubmissionStatus(submissionId, SubmissionStatus.REJECTED, formOperationFailed.metadata());
    }

    @Handles("public.progression.cotr-operation-failed")
    public void cotrFormOperationFailed(final Envelope<CotrOperationFailed> cotrOperationFailed) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("public.progression.cotr-operation-failed event with cotrId {}", cotrOperationFailed.payload().getCotrId());
        }
        final CotrOperationFailed cotrOperationFailedPayload = cotrOperationFailed.payload();

        if (isNull(cotrOperationFailedPayload) || isNull(cotrOperationFailedPayload.getSubmissionId())) {
            LOGGER.info("Submission ID not found. Cotr Form rejected event ignored");
            return;
        }

        final Optional<UUID> submissionId = ofNullable(cotrOperationFailedPayload.getSubmissionId());
        final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem> errors = new ArrayList<>();
        final String message  = cotrOperationFailedPayload.getMessage();

        if(nonNull(message)) {
            final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue> values = new ArrayList<>();
            final uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue problemValue = uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue.problemValue()
                    .withKey("CASE_ID")
                    .withValue(cotrOperationFailedPayload.getCaseId().toString())
                    .build();
            values.add(problemValue);

            final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem.Builder problem = uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem.problem()
                    .withValues(values);
            if (COTR_ID_NOT_FOUND.equals(message)) {
                problem.withCode("FORM_DOES_NOT_EXIST");
            } else if (HEARING_ID_NOT_FOUND.equalsIgnoreCase(message)) {
                problem.withCode("PROVIDE_HEARING_DETAILS");
            }
            errors.add(problem.build());
        }

        LOGGER.info("Raise stagingprosecutors.command.update-submission-status with submission id {}", submissionId);
        final UpdateSubmissionStatus commandPayload = UpdateSubmissionStatus.updateSubmissionStatus()
                .withSubmissionId(submissionId.get())
                .withSubmissionStatus(SubmissionStatus.REJECTED)
                .withErrors(errors)
                .build();

        final Metadata metadata = metadataFrom(cotrOperationFailed.metadata())
                .withName(UPDATE_SUBMISSION_STATUS_COMMAND)
                .build();
        final Envelope<UpdateSubmissionStatus> updateSubmissionStatusEnvelope = envelopeFrom(metadata, commandPayload);
        final UpdateSubmissionStatus updateSubmissionStatus = updateSubmissionStatusEnvelope.payload();
        LOGGER.info("raising stagingprosecutors.command.update-submission-status command: {}", updateSubmissionStatus.getSubmissionStatus());

        sender.send(updateSubmissionStatusEnvelope);
    }


    @Handles("public.progression.form-created")
    public void formCreated(final Envelope<FormCreated> envelope) {
        LOGGER.info("public.progression.form-created event ...");

        final Optional<UUID> submissionId = ofNullable(
                envelope.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null))
                .map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info("Submission ID not found. Form created event ignored");
            return;
        }

        updateSucessSubmissionStatus(submissionId, SubmissionStatus.SUCCESS, envelope.metadata());
    }

    @Handles("public.progression.pet-form-created")
    public void petFormCreated(final Envelope<PetFormCreated> envelope) {
        LOGGER.info("public.progression.pet-form-created event ...");

        final Optional<UUID> submissionId = Optional.ofNullable(envelope.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null)).map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info("Submission ID not found. PET Form created event ignored");
            return;
        }

        updateSucessSubmissionStatus(submissionId, SubmissionStatus.SUCCESS, envelope.metadata());

    }

    @Handles("public.progression.cotr-created")
    public void cotrCreated(final Envelope<CotrCreated> envelope) {
        LOGGER.info("public.progression.cotr-created event ...");

        final Optional<UUID> submissionId = ofNullable(envelope.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null)).map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info("Submission ID not found. COTR Form created event ignored");
            return;
        }

        updateSucessSubmissionStatus(submissionId, SubmissionStatus.SUCCESS, envelope.metadata());

    }

    @Handles("public.progression.cotr-review-notes-updated")
    public void cotrUpdateReviewStatusSuccessStatus(final Envelope<CotrReviewNotesUpdated> envelope) {
        LOGGER.info("public.progression.cotr-review-notes-updated event ...");

        final Optional<UUID> submissionId = Optional.ofNullable(envelope.metadata().asJsonObject().getString(SUBMISSION_ID_STR, null)).map(UUID::fromString);

        if (!submissionId.isPresent()) {
            LOGGER.info("Submission ID not found. COTR review notes updated event ignored");
            return;
        }

        updateSucessSubmissionStatus(submissionId, SubmissionStatus.SUCCESS, envelope.metadata());

    }

    @SuppressWarnings("squid:S3655")
    private void updateSucessSubmissionStatus(Optional<UUID> submissionId, SubmissionStatus success, Metadata metadata2) {
        LOGGER.info("Raise stagingprosecutors.command.update-submission-status with submission id {}", submissionId);
        final UpdateSubmissionStatus commandPayload = UpdateSubmissionStatus.updateSubmissionStatus()
                .withSubmissionId(submissionId.get())
                .withSubmissionStatus(success)
                .build();

        final Metadata metadata = metadataFrom(metadata2)
                .withName(UPDATE_SUBMISSION_STATUS_COMMAND)
                .build();
        final Envelope<UpdateSubmissionStatus> updateSubmissionStatusEnvelope = envelopeFrom(metadata, commandPayload);
        final UpdateSubmissionStatus updateSubmissionStatus = updateSubmissionStatusEnvelope.payload();
        LOGGER.info("raising stagingprosecutors.command.update-submission-status command: {}", updateSubmissionStatus.getSubmissionStatus());

        sender.send(updateSubmissionStatusEnvelope);
    }

    @Handles("public.progression.court-application-proceedings-initiated")
    public void notifyCourtApplicationCreated(final JsonEnvelope initiateCourtProceedings) {
        if (featureControlGuard.isFeatureEnabled(FEATURE_DEFENCE_DISCLOSURE)) {
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("handling public.progression.court-application-created: {}", initiateCourtProceedings.toObfuscatedDebugString());
            }
            final CourtApplicationWithCaseDto courtApplicationWithCaseDto = jsonObjectToObjectConverter.convert(initiateCourtProceedings.payloadAsJsonObject(), CourtApplicationWithCaseDto.class);
            final CourtApplication courtApplication = courtApplicationWithCaseDto.getCourtApplication();
            final List<UUID> cpsProsecutorsList = toProsecutorIdList(referenceDataQueryService.getCPSProsecutors(initiateCourtProceedings, requester));
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("Received CPS Prosecutors List: {} and payLoad: {}", cpsProsecutorsList, initiateCourtProceedings.toObfuscatedDebugString());
            }
            if (ofNullable(courtApplication.getApplicationExternalCreatorType()).isPresent() && ApplicationExternalCreatorType.PROSECUTOR.equals(ofNullable(courtApplication.getApplicationExternalCreatorType()).get())) {
                final ApplicationRequestNotification applicationRequestNotification = ApplicationRequestNotification.applicationRequestNotification()
                        .withApplicationId(courtApplication.getId().toString())
                        .withApplicationStatus(ApplicationStatus.CREATED.name())
                        .build();
                final JsonObject payload = cpsPayloadTransformService.transformApplicationRequestNotification(applicationRequestNotification, "application-request-status");
                LOGGER.info("Sending to Notification when Application has Cps Respondent with applicationId {}" , applicationRequestNotification.getApplicationId());
                final ResponseDto responseDto = cpsApiService.sendApplicationCreatedNotification(payload, "PATCH");

                LOGGER.info("Application has Cps Applicant: Received response: {}", responseDto.getStatusCode());
            } else {
                final boolean applicantCPS = isApplicantCPS(courtApplication, cpsProsecutorsList);
                final boolean respondentCps = isRespondentCps(courtApplication, cpsProsecutorsList);
                if (applicantCPS || respondentCps) {
                    LOGGER.info("Application has Cps Applicant  {}", applicantCPS);
                    LOGGER.info("Application has Cps Respondent  {}", respondentCps);


                    final JsonObject payload = cpsPayloadTransformService.transformCourtApplicationNotification(courtApplicationWithCaseDto, "application-created", cpsProsecutorsList);

                    LOGGER.info("Sending to Notification when Application has Cps Respondent with courtApplication Id : {}" , courtApplicationWithCaseDto.getCourtApplication().getId());
                    final ResponseDto responseDto = cpsApiService.sendApplicationCreatedNotification(payload, "POST");

                    LOGGER.info("Application has Cps Applicant: Received response: {}", responseDto.getStatusCode());
                }
            }
        }else{
            LOGGER.info("enable defenceDisclosure feature to send application notification to CPE");
        }
    }

    @Handles("public.prosecutioncasefile.submit-application-validation-failed")
    public void handleApplicationValidationFailed(final Envelope<SubmitApplicationValidationFailed> envelope) {
        final SubmitApplicationValidationFailed submitApplicationValidationFailed = envelope.payload();
        final ApplicationRequestNotification applicationRequestNotification = buildApplicationRequestNotification(submitApplicationValidationFailed);
        final JsonObject payload = cpsPayloadTransformService.transformApplicationRequestNotification(applicationRequestNotification, "application-request-status");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Sending to Notification when Application has Cps Respondent with ApplicationId : {}", applicationRequestNotification.getApplicationId());
        }
        final ResponseDto responseDto = cpsApiService.sendApplicationCreatedNotification(payload, "PATCH");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Application has Cps Respondent: Received response: {}", responseDto.getStatusCode());
        }

    }

    private boolean isApplicantCPS(final CourtApplication courtApplication, final List<UUID> cpsProsecutorIdList) {
        LOGGER.info("Received  Court Application with Id= {}",  courtApplication.getId());

        LOGGER.info("is Applicant check {}", nonNull(courtApplication.getApplicant())
                && ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).isPresent()
                && nonNull(ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).get().getProsecutionAuthorityId())
                && cpsProsecutorIdList.contains(ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).get().getProsecutionAuthorityId()));
        return nonNull(courtApplication.getApplicant())
                && ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).isPresent()
                && nonNull(ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).get().getProsecutionAuthorityId())
                && cpsProsecutorIdList.contains(ofNullable(courtApplication.getApplicant().getProsecutingAuthority()).get().getProsecutionAuthorityId());
    }

    private boolean isRespondentCps(final CourtApplication courtApplication, final List<UUID> cpsProsecutorIdList) {
        return nonNull(courtApplication.getRespondents())
                && !courtApplication.getRespondents().isEmpty()
                && courtApplication.getRespondents().stream()
                .filter(resp -> nonNull(resp.getProsecutingAuthority()))
                .filter(resp -> ofNullable(resp.getProsecutingAuthority()).isPresent())
                .anyMatch(resp -> cpsProsecutorIdList.contains(ofNullable(resp.getProsecutingAuthority()).get().getProsecutionAuthorityId()));
    }

    private List<UUID> toProsecutorIdList(Optional<JsonArray> cpsProsecutorsJsonArray) {
        return cpsProsecutorsJsonArray
                .map(jsonValues -> jsonValues.stream()
                        .map(p -> ((JsonObject) p).getString("id"))
                        .map(UUID::fromString)
                        .collect(Collectors.toList()))
                .orElse(emptyList());
    }

    private ApplicationRequestNotification buildApplicationRequestNotification(final SubmitApplicationValidationFailed submitApplicationValidationFailed) {
        final String applicationId = submitApplicationValidationFailed.getApplicationSubmitted().getCourtApplication().getId().toString();
        String validationErrorType = submitApplicationValidationFailed.getErrorDetails().getErrorDetails().stream()
                .filter(error -> error.getErrorCode().equals(DEFENDANT_ASN_NOT_FOUND.getCode()) || error.getErrorCode().equals(DEFENDANT_DETAILS_NOT_FOUND.getCode()))
                .map(Error::getErrorCode)
                .findFirst().orElse(null);
        if (validationErrorType == null) {
            validationErrorType = submitApplicationValidationFailed.getErrorDetails().getErrorDetails().get(0).getErrorCode();
        }
        final ApplicationRequestNotification.Builder notificationBuilder = applicationRequestNotification();

        notificationBuilder
                .withApplicationId(applicationId)
                .withApplicationStatus(ERROR.toString())
                .withValidationErrorType(validationErrorType);

        return getDefendant(notificationBuilder, validationErrorType, submitApplicationValidationFailed).build();

    }

    private ApplicationRequestNotification.Builder getDefendant(ApplicationRequestNotification.Builder notificationBuilder, final String validationErrorType, final SubmitApplicationValidationFailed validationFailed) {
        if (validationErrorType.equals(DEFENDANT_ASN_NOT_FOUND.getCode()) || validationErrorType.equals(DEFENDANT_DETAILS_NOT_FOUND.getCode())) {
            validationFailed.getApplicationSubmitted().getProsecutionCases()
                    .forEach(prosecutionCase -> {
                        final String prosecutorDefendantId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                        final List<CaseDefendantOrganisation> caseDefendantOrganisationList = getCaseDefendantOrganisations(prosecutionCase.getDefendants(), prosecutorDefendantId);
                        final List<CaseDefendantIndividual> caseDefendantIndividualList = getCaseDefendantIndividuals(prosecutionCase.getDefendants(), prosecutorDefendantId);
                        if (!caseDefendantIndividualList.isEmpty()) {
                            notificationBuilder.withCaseDefendantIndividuals(caseDefendantIndividualList);
                        }
                        if (!caseDefendantOrganisationList.isEmpty()) {
                            notificationBuilder.withCaseDefendantOrganisations(caseDefendantOrganisationList);
                        }
                    });
        }
        return notificationBuilder;
    }

    private List<CaseDefendantOrganisation> getCaseDefendantOrganisations(final List<Defendant> defendantList, final String prosecutorDefendantId) {
        return defendantList.stream().map(defendant -> {
            final CaseDefendantOrganisation.Builder caseDefendantOrganisation = CaseDefendantOrganisation.caseDefendantOrganisation();
            if (nonNull(defendant.getLegalEntityDefendant())) {
                final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
                caseDefendantOrganisation
                        .withName(legalEntityDefendant.getOrganisation().getName())
                        .withProsecutorDefendantId(prosecutorDefendantId);
                return caseDefendantOrganisation.build();
            }
            return null;
        }).filter(Objects::nonNull).collect(toList());
    }

    private List<CaseDefendantIndividual> getCaseDefendantIndividuals(final List<Defendant> defendantList, final String prosecutorDefendantId) {
        return defendantList.stream().map(defendant -> {
            final CaseDefendantIndividual.Builder defendantIndividual = CaseDefendantIndividual.caseDefendantIndividual();
            if (nonNull(defendant.getPersonDefendant())) {
                final Person personDetails = defendant.getPersonDefendant().getPersonDetails();
                defendantIndividual
                        .withAsn(defendant.getPersonDefendant().getArrestSummonsNumber())
                        .withProsecutorDefendantId(prosecutorDefendantId)
                        .withTitle(personDetails.getTitle())
                        .withForename(personDetails.getFirstName())
                        .withForename2(personDetails.getMiddleName())
                        .withSurname(personDetails.getLastName())
                        .withDateOfBirth(personDetails.getDateOfBirth() != null ? parse(personDetails.getDateOfBirth()) : null);
                return defendantIndividual.build();
            }
            return null;
        }).filter(Objects::nonNull).collect(toList());
    }
}
