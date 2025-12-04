package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDocument;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProblemValue;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorOrganisationDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutorPersonDefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsMaterialSubmitted;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialV3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialSubmissionTest {

    @InjectMocks
    private MaterialSubmission materialSubmission;

    @Test
    public void shouldSubmitMaterialV3ForNonCPSCase() {

        final SubmitMaterialV3 submitMaterialV3 = createSubmitMaterial(false, false);

        final UUID submissionId = randomUUID();

        final Stream<Object> eventStream = materialSubmission.submitMaterialV3(submissionId, submitMaterialV3);

        final MaterialSubmittedV3 materialSubmitted = (MaterialSubmittedV3) eventStream.findFirst().get();
        assertThat(materialSubmitted.getSubmissionId(), is(submissionId));
        assertThat(materialSubmitted.getIsCpsCase(), is(false));

        final ProsecutionCaseSubject actualProsecutionCaseSubject = submitMaterialV3.getProsecutionCaseSubject();
        final ProsecutionCaseSubject expectedProsecutionCaseSubject = materialSubmitted.getProsecutionCaseSubject();

        assertThat(actualProsecutionCaseSubject.getCaseUrn(), is(expectedProsecutionCaseSubject.getCaseUrn()));
        assertThat(actualProsecutionCaseSubject.getProsecutingAuthority(), is(expectedProsecutionCaseSubject.getProsecutingAuthority()));

        final DefendantSubject actualDefendantSubject = submitMaterialV3.getProsecutionCaseSubject().getDefendantSubject();
        final DefendantSubject expectedDefendantSubject = materialSubmitted.getProsecutionCaseSubject().getDefendantSubject();

        assertThat(actualDefendantSubject.getProsecutorDefendantId(), is(expectedDefendantSubject.getProsecutorDefendantId()));
        assertThat(actualDefendantSubject.getProsecutorPersonDefendantDetails().getForename2(),
                is(expectedDefendantSubject.getProsecutorPersonDefendantDetails().getForename2()));
        assertThat(actualDefendantSubject.getProsecutorPersonDefendantDetails().getProsecutorDefendantId(),
                is(expectedDefendantSubject.getProsecutorPersonDefendantDetails().getProsecutorDefendantId()));
        assertThat(actualDefendantSubject.getCpsPersonDefendantDetails(), is(nullValue()));
        assertThat(expectedDefendantSubject.getAsn(), is(nullValue()));
    }

    @Test
    public void shouldSubmitMaterialV3ForCPSCase() {

        final SubmitMaterialV3 submitMaterialV3 = createSubmitMaterial(true, false);
        final UUID submissionId = randomUUID();

        final Stream<Object> eventStream = materialSubmission.submitMaterialV3(submissionId, submitMaterialV3);

        final MaterialSubmittedV3 materialSubmitted = (MaterialSubmittedV3) eventStream.findFirst().get();
        assertThat(materialSubmitted.getSubmissionId(), is(submissionId));
        assertThat(materialSubmitted.getIsCpsCase(), is(true));

        final ProsecutionCaseSubject actualProsecutionCaseSubject = submitMaterialV3.getProsecutionCaseSubject();
        final ProsecutionCaseSubject expectedProsecutionCaseSubject = materialSubmitted.getProsecutionCaseSubject();

        assertThat(actualProsecutionCaseSubject.getCaseUrn(), is(expectedProsecutionCaseSubject.getCaseUrn()));
        assertThat(actualProsecutionCaseSubject.getProsecutingAuthority(), is(expectedProsecutionCaseSubject.getProsecutingAuthority()));

        final DefendantSubject actualDefendantSubject = submitMaterialV3.getProsecutionCaseSubject().getDefendantSubject();
        final DefendantSubject expectedDefendantSubject = materialSubmitted.getProsecutionCaseSubject().getDefendantSubject();

        assertThat(actualDefendantSubject.getCpsDefendantId(), is(expectedDefendantSubject.getCpsDefendantId()));
        assertThat(actualDefendantSubject.getCpsPersonDefendantDetails().getForename2(),
                is(expectedDefendantSubject.getCpsPersonDefendantDetails().getForename2()));
        assertThat(actualDefendantSubject.getCpsPersonDefendantDetails().getCpsDefendantId(),
                is(expectedDefendantSubject.getCpsPersonDefendantDetails().getCpsDefendantId()));
        assertThat(actualDefendantSubject.getProsecutorPersonDefendantDetails(), is(nullValue()));
        assertThat(actualDefendantSubject.getAsn(), is(nullValue()));

    }

    @Test
    public void shouldSubmitMaterialV3WithDefendantSubjectWithAsn() {

        final SubmitMaterialV3 submitMaterialV3 = createSubmitMaterial(false, true);

        final UUID submissionId = randomUUID();

        final Stream<Object> eventStream = materialSubmission.submitMaterialV3(submissionId, submitMaterialV3);

        final MaterialSubmittedV3 materialSubmitted = (MaterialSubmittedV3) eventStream.findFirst().get();
        assertThat(materialSubmitted.getSubmissionId(), is(submissionId));
        assertThat(materialSubmitted.getIsCpsCase(), is(false));

        final DefendantSubject actualDefendantSubject = submitMaterialV3.getProsecutionCaseSubject().getDefendantSubject();
        final DefendantSubject expectedDefendantSubject = materialSubmitted.getProsecutionCaseSubject().getDefendantSubject();

        assertThat(actualDefendantSubject.getAsn(), is(expectedDefendantSubject.getAsn()));
        assertThat(actualDefendantSubject.getProsecutorPersonDefendantDetails(), is(nullValue()));
        assertThat(actualDefendantSubject.getCpsPersonDefendantDetails(), is(nullValue()));
    }


    private SubmitMaterialV3 createSubmitMaterial(final boolean cpsFlag, final boolean asnFlag) {

        final CpsPersonDefendantDetails cpsPersonDefendantDetails = new CpsPersonDefendantDetails.Builder()
                .withForename(STRING.next())
                .withCpsDefendantId(randomUUID().toString())
                .build();

        final ProsecutorPersonDefendantDetails prosecutorPersonDefendantDetails = new ProsecutorPersonDefendantDetails.Builder()
                .withForename2(STRING.next())
                .withProsecutorDefendantId(randomUUID().toString())
                .build();

        final ProsecutorOrganisationDefendantDetails prosecutorOrganisationDefendantDetails = new ProsecutorOrganisationDefendantDetails.Builder()
                .withProsecutorDefendantId(randomUUID().toString())
                .withOrganisationName(STRING.next())
                .build();

        final CpsOrganisationDefendantDetails cpsOrganisationDefendantDetails = new CpsOrganisationDefendantDetails.Builder()
                .withCpsDefendantId(randomUUID().toString())
                .withOrganisationName(STRING.next())
                .build();

        final DefendantSubject.Builder defendantSubject = new DefendantSubject.Builder();

        if (cpsFlag && !asnFlag) {
            defendantSubject.withCpsDefendantId(randomUUID().toString());
            defendantSubject.withCpsPersonDefendantDetails(cpsPersonDefendantDetails);
            defendantSubject.withCpsOrganisationDefendantDetails(cpsOrganisationDefendantDetails);
        } else if (!cpsFlag && !asnFlag) {
            defendantSubject.withProsecutorDefendantId(randomUUID().toString());
            defendantSubject.withProsecutorPersonDefendantDetails(prosecutorPersonDefendantDetails);
            defendantSubject.withProsecutorOrganisationDefendantDetails(prosecutorOrganisationDefendantDetails);
        } else {
            defendantSubject.withAsn(STRING.next());
        }

        final ProsecutionCaseSubject prosecutionCaseSubject = new ProsecutionCaseSubject.Builder()
                .withCaseUrn(STRING.next())
                .withProsecutingAuthority(STRING.next())
                .withDefendantSubject(defendantSubject.build())
                .build();

        final SubmitMaterialV3 submitMaterial = new SubmitMaterialV3.Builder()
                .withProsecutionCaseSubject(prosecutionCaseSubject)
                .build();
        return submitMaterial;
    }

    @Test
    public void shouldSubmitMaterial() {

        final UUID submissionId = randomUUID();
        final UUID materialId = randomUUID();
        final String caseUrn = "caseUrn";
        final String prosecutingAuthority = "prosecutingAuthority";
        final String materialType = "materialType";
        final java.util.Optional<String> defendantId = of(randomUUID().toString());
        final Optional<Boolean> isCpsCase = of(false);

        final Stream<Object> eventStream = materialSubmission.submitMaterial(submissionId, materialId, caseUrn, prosecutingAuthority, materialType, defendantId, isCpsCase);

        final MaterialSubmitted materialSubmitted = (MaterialSubmitted) eventStream.findFirst().get();
        assertThat(materialSubmitted.getSubmissionId(), is(submissionId));
        assertThat(materialSubmitted.getMaterialId(), is(materialId));
        assertThat(materialSubmitted.getCaseUrn(), is(caseUrn));
        assertThat(materialSubmitted.getProsecutingAuthority(), is(prosecutingAuthority));
        assertThat(materialSubmitted.getMaterialType(), is(materialType));
        assertThat(materialSubmitted.getSubmissionStatus(), is(PENDING));
        assertThat(materialSubmitted.getDefendantId(), is(defendantId.get()));
        assertThat(materialSubmitted.getIsCpsCase(), is(false));

    }

    @Test
    public void shouldSubmitCpsMaterial() {

        final UUID submissionId = randomUUID();
        final Integer transactionID = 1;
        final String caseUrn = "caseUrn";
        final Integer compassCaseId = 2;
        final java.util.Optional<String> responseEmail = of("responseemail@hmcts.net");
        final List<CpsDocument> documents = singletonList(CpsDocument.cpsDocument().withDocumentId(randomUUID().toString()).build());
        final List<CpsDefendant> defendants = singletonList(CpsDefendant.cpsDefendant().withDefendantID(randomUUID().toString()).build());

        final Stream<Object> eventStream = materialSubmission.submitCpsMaterial(submissionId, transactionID, caseUrn, compassCaseId, responseEmail, defendants, documents);

        final CpsMaterialSubmitted cpsMaterialSubmitted = (CpsMaterialSubmitted) eventStream.findFirst().get();
        assertThat(cpsMaterialSubmitted.getSubmissionId(), is(submissionId));
        assertThat(cpsMaterialSubmitted.getCompassCaseId(), is(compassCaseId));
        assertThat(cpsMaterialSubmitted.getUrn(), is(caseUrn));
        assertThat(cpsMaterialSubmitted.getDefendants(), is(defendants));
        assertThat(cpsMaterialSubmitted.getDocuments(), is(documents));
        assertThat(cpsMaterialSubmitted.getSubmissionStatus().toString(), is(PENDING.toString()));
        assertThat(cpsMaterialSubmitted.getTransactionID(), is(transactionID));
        assertThat(cpsMaterialSubmitted.getResponseEmail(), is(responseEmail.get()));

    }

    @Test
    public void shouldReceiveMaterialSubmissionSuccessful() {
        final UUID submissionId = randomUUID();

        final Stream<Object> eventStream = materialSubmission.receiveMaterialSubmissionSuccessful(submissionId);

        final MaterialSubmissionSuccessful materialSubmissionSuccessful = (MaterialSubmissionSuccessful) eventStream.findFirst().get();
        assertThat(materialSubmissionSuccessful.getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldRejectMaterial() {
        final UUID submissionId = randomUUID();

        final ProblemValue problemValue = ProblemValue.problemValue()
                .withId(randomUUID().toString())
                .withKey("key1")
                .withValue("value1")
                .build();
        final Problem problem = Problem.problem()
                .withCode("errCode1")
                .withValues(singletonList(problemValue))
                .build();
        final List<Problem> errors = singletonList(problem);

        materialSubmission.apply(MaterialSubmitted.materialSubmitted().withSubmissionId(submissionId).build());
        final Stream<Object> eventStream = materialSubmission.rejectMaterial(errors, null);

        final MaterialSubmissionRejected materialSubmissionRejected = (MaterialSubmissionRejected) eventStream.findFirst().get();
        assertThat(materialSubmissionRejected.getSubmissionId(), is(submissionId));
        assertThat(materialSubmissionRejected.getErrors(), is(errors));
    }

    @Test
    public void shouldMaterialPendingWithWarning() {
        final UUID submissionId = randomUUID();

        final ProblemValue problemValue = ProblemValue.problemValue()
                .withId(randomUUID().toString())
                .withKey("key1")
                .withValue("value1")
                .build();
        final Problem problem = Problem.problem()
                .withCode("errCode1")
                .withValues(singletonList(problemValue))
                .build();
        final List<Problem> warnings = singletonList(problem);

        materialSubmission.apply(MaterialSubmitted.materialSubmitted().withSubmissionId(submissionId).build());
        final Stream<Object> eventStream = materialSubmission.materialPendingWithWarning(warnings);

        final SubmissionPendingWithWarnings submissionPendingWithWarnings = (SubmissionPendingWithWarnings) eventStream.findFirst().get();
        assertThat(submissionPendingWithWarnings.getSubmissionId(), is(submissionId));
        assertThat(submissionPendingWithWarnings.getWarnings(), is(warnings));
    }

}
