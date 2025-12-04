package uk.gov.moj.cpp.staging.prosecutors.domain;

import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DocumentFailedToUnbundle;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnbundleSubmission implements Aggregate {

    private static final long serialVersionUID = 1L;

    private UUID caseId;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(DocumentFailedToUnbundle.class).apply(e -> caseId = e.getCaseId()),
                when(DocumentUnbundled.class).apply(e -> caseId = e.getCaseId()),
                when(DocumentUnbundledV2.class).apply(e -> caseId = e.getCaseId()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> submitUnbundleResultRecord(
            final UUID caseId,
            final String prosecutorDefendantId,
            final Optional<String> prosecutingAuthority,
            final Optional<ZonedDateTime> receivedDateTime,
            final Optional<Material> material,
            final Optional<String> errorMessage) {

        if (errorMessage.isPresent()) {
            return apply(Stream.of(
                    DocumentFailedToUnbundle.documentFailedToUnbundle()
                            .withCaseId(caseId)
                            .withProsecutorDefendantId(prosecutorDefendantId)
                            .withProsecutingAuthority(prosecutingAuthority.orElse(null))
                            .withErrorMessage(errorMessage.get())
                            .build()
            ));
        } else {
            final DocumentUnbundled.Builder documentUnbundledBuilder = DocumentUnbundled.documentUnbundled()
                    .withCaseId(caseId)
                    .withProsecutorDefendantId(prosecutorDefendantId)
                    .withProsecutingAuthority(prosecutingAuthority);
            material.ifPresent(materialValue -> documentUnbundledBuilder.withMaterial(prosecutionCaseObjectToDomain(materialValue)));
            receivedDateTime.ifPresent(documentUnbundledBuilder::withReceivedDateTime);
            return apply(Stream.of(documentUnbundledBuilder.build()));
        }
    }

    public Stream<Object> submitUnbundleDocumentResults(final UUID caseId,
                                                        final String prosecutorDefendantId,
                                                        final String prosecutingAuthority,
                                                        final ZonedDateTime receivedDateTime,
                                                        final List<Material> materials,
                                                        final String errorMessage) {

        if (nonNull(errorMessage)) {
            return apply(Stream.of(
                    DocumentFailedToUnbundle.documentFailedToUnbundle()
                            .withCaseId(caseId)
                            .withProsecutorDefendantId(prosecutorDefendantId)
                            .withProsecutingAuthority(nonNull(prosecutingAuthority) ? prosecutingAuthority : null)
                            .withErrorMessage(errorMessage)
                            .build()));
        } else {
            if (CollectionUtils.isNotEmpty(materials)) {
                final Stream.Builder<Object> streamBuilder = Stream.builder();
                final DocumentUnbundledV2.Builder documentUnbundledBuilder = DocumentUnbundledV2.documentUnbundled()
                        .withCaseId(caseId)
                        .withProsecutorDefendantId(prosecutorDefendantId)
                        .withProsecutingAuthority(Optional.ofNullable(prosecutingAuthority))
                        .withMaterials(materials.stream().map(this::prosecutionCaseObjectToDomain).collect(Collectors.toList()));
                if (nonNull(receivedDateTime)) {
                    documentUnbundledBuilder.withReceivedDateTime(receivedDateTime);
                }
                streamBuilder.add(documentUnbundledBuilder.build());
                return apply(streamBuilder.build());
            }
        }
        return Stream.empty();
    }

    private uk.gov.moj.cpp.staging.prosecutors.domain.Material prosecutionCaseObjectToDomain(final Material material) {
        return uk.gov.moj.cpp.staging.prosecutors.domain.Material.material()
                .withFileStoreId(material.getFileStoreId())
                .withFileType(material.getFileType())
                .withIsUnbundledDocument(true)
                .withDocumentType(material.getDocumentType())
                .build();
    }
}
