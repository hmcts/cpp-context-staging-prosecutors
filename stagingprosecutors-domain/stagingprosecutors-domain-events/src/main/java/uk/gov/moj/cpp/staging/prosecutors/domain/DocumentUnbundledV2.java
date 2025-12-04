package uk.gov.moj.cpp.staging.prosecutors.domain;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * DocumentUnbundledV2 event to represent multiple Materials unbundled as opposed to {@link DocumentUnbundled} which contains
 * single event.
 */
@SuppressWarnings("squid:S2384")
@Event("stagingprosecutors.event.document-unbundled-v2")
public class DocumentUnbundledV2 {
    private final UUID caseId;

    private final List<Material> materials;

    private final Optional<String> prosecutingAuthority;

    private final String prosecutorDefendantId;

    private final ZonedDateTime receivedDateTime;

    public DocumentUnbundledV2(final UUID caseId, final List<Material> materials, final Optional<String> prosecutingAuthority, final String prosecutorDefendantId, final ZonedDateTime receivedDateTime) {
        this.caseId = caseId;
        this.materials = materials;
        this.prosecutingAuthority = prosecutingAuthority;
        this.prosecutorDefendantId = prosecutorDefendantId;
        this.receivedDateTime = receivedDateTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public Optional<String> getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getProsecutorDefendantId() {
        return prosecutorDefendantId;
    }

    public ZonedDateTime getReceivedDateTime() {
        return receivedDateTime;
    }

    public static Builder documentUnbundled() {
        return new DocumentUnbundledV2.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseId, materials, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
    }

    @Override
    public String toString() {
        return "DocumentUnbundled{" +
                "caseId='" + caseId + "'," +
                "materials='" + materials + "'," +
                "prosecutingAuthority='" + prosecutingAuthority + "'," +
                "prosecutorDefendantId='" + prosecutorDefendantId + "'," +
                "receivedDateTime='" + receivedDateTime + "'" +
                "}";
    }

    public static class Builder {
        private UUID caseId;

        private List<Material> materials;

        private Optional<String> prosecutingAuthority;

        private String prosecutorDefendantId;

        private ZonedDateTime receivedDateTime;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withMaterials(final List<Material> materials) {
            this.materials = materials;
            return this;
        }

        public Builder withProsecutingAuthority(final Optional<String> prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
            return this;
        }

        public Builder withProsecutorDefendantId(final String prosecutorDefendantId) {
            this.prosecutorDefendantId = prosecutorDefendantId;
            return this;
        }

        public Builder withReceivedDateTime(final ZonedDateTime receivedDateTime) {
            this.receivedDateTime = receivedDateTime;
            return this;
        }

        public DocumentUnbundledV2 build() {
            return new DocumentUnbundledV2(caseId, materials, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
        }
    }
}
