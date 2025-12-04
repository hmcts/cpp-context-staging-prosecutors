package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionRejected.materialSubmissionRejected;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmissionSuccessful.materialSubmissionSuccessful;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionPendingWithWarnings.submissionPendingWithWarnings;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SubmissionStatus.PENDING;
import static cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3.materialSubmittedV3;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDocument;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MaterialSubmitted;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCaseSubject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import cpp.moj.gov.uk.staging.prosecutors.json.schemas.CpsMaterialSubmitted;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.MaterialSubmittedV3;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitMaterialV3;

public class MaterialSubmission implements Aggregate {

    private static final long serialVersionUID = 1L;

    private UUID submissionId;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(MaterialSubmitted.class).apply(e -> submissionId = e.getSubmissionId()),
                when(CpsMaterialSubmitted.class).apply(e -> submissionId = e.getSubmissionId()),
                when(MaterialSubmittedV3.class).apply(e -> submissionId = e.getSubmissionId()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> submitMaterial(final UUID submissionId,
                                         final UUID materialId,
                                         final String caseUrn,
                                         final String prosecutingAuthority,
                                         final String materialType,
                                         final Optional<String> defendantId, Optional<Boolean> isCpsCase) {

        return apply(Stream.of(MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withMaterialId(materialId)
                .withCaseUrn(caseUrn)
                .withProsecutingAuthority(prosecutingAuthority)
                .withMaterialType(materialType)
                .withSubmissionStatus(PENDING)
                .withDefendantId(defendantId.orElse(null))
                .withIsCpsCase(isCpsCase.isPresent() ? isCpsCase.get().booleanValue() : FALSE)
                .build()));
    }

    public Stream<Object> submitCpsMaterial(final UUID submissionId,
                                            final Integer transactionID,
                                            final String urn,
                                            final Integer compassCaseId,
                                            final Optional<String> responseEmail,
                                            final List<CpsDefendant> defendants, final List<CpsDocument> documents) {

        return apply(Stream.of(CpsMaterialSubmitted.cpsMaterialSubmitted()
                .withCompassCaseId(compassCaseId)
                .withDefendants(defendants)
                .withDocuments(documents)
                .withResponseEmail(responseEmail.orElse(null))
                .withSubmissionId(submissionId)
                .withSubmissionStatus(cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus.PENDING)
                .withTransactionID(transactionID)
                .withUrn(urn)
                .build()));
    }

    public Stream<Object> receiveMaterialSubmissionSuccessful(final UUID submissionId) {
        return apply(of(materialSubmissionSuccessful()
                .withSubmissionId(submissionId)
                .build()));
    }

    public Stream<Object> rejectMaterial(final List<Problem> errors, final List<Problem> warnings) {
        return apply(of(materialSubmissionRejected()
                .withSubmissionId(submissionId)
                .withErrors(errors)
                .withWarnings(warnings)
                .build()));
    }

    public Stream<Object> materialPendingWithWarning(final List<Problem> warnings) {
        return apply(of(submissionPendingWithWarnings()
                .withSubmissionId(submissionId)
                .withWarnings(warnings)
                .build()));
    }

    public Stream<Object> submitMaterialV3(final UUID submissionId, final SubmitMaterialV3 submitMaterialV3) {

        boolean cpsFlag = false;

        final ProsecutionCaseSubject prosecutionCaseSubject = submitMaterialV3.getProsecutionCaseSubject();
        if (prosecutionCaseSubject != null) {
            final DefendantSubject defendantSubject = prosecutionCaseSubject.getDefendantSubject();
            if (defendantSubject != null && (defendantSubject.getCpsPersonDefendantDetails() != null ||
                    defendantSubject.getCpsDefendantId() != null || defendantSubject.getCpsOrganisationDefendantDetails() != null)) {
                cpsFlag = true;
            }
        }
        return apply(of(materialSubmittedV3()
                .withSubmissionId(submissionId)
                .withMaterial(submitMaterialV3.getMaterial())
                .withMaterialContentType(submitMaterialV3.getMaterialContentType())
                .withMaterialName(submitMaterialV3.getMaterialName())
                .withMaterialType(submitMaterialV3.getMaterialType())
                .withSectionOrderSequence(submitMaterialV3.getSectionOrderSequence())
                .withFileName(submitMaterialV3.getFileName())
                .withCaseSubFolderName(submitMaterialV3.getCaseSubFolderName())
                .withCourtApplicationSubject(submitMaterialV3.getCourtApplicationSubject())
                .withProsecutionCaseSubject(submitMaterialV3.getProsecutionCaseSubject())
                .withExhibit(submitMaterialV3.getExhibit())
                .withWitnessStatement(submitMaterialV3.getWitnessStatement())
                .withTag(submitMaterialV3.getTag())
                .withIsCpsCase(cpsFlag)
                .withSubmissionStatus(cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmissionStatus.PENDING)
                .build()));
    }
}
