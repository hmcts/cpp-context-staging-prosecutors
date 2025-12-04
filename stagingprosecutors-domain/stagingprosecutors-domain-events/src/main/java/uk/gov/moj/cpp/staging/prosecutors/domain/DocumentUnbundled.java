package uk.gov.moj.cpp.staging.prosecutors.domain;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * This event is legacy event and is now replaced by {@link DocumentUnbundledV2}.
 */
@Event("stagingprosecutors.event.document-unbundled")
public class DocumentUnbundled {
    private final UUID caseId;

    private final Material material;

    private final Optional<String> prosecutingAuthority;

    private final String prosecutorDefendantId;

    private final ZonedDateTime receivedDateTime;

    public DocumentUnbundled(final UUID caseId, final Material material, final Optional<String> prosecutingAuthority, final String prosecutorDefendantId, final ZonedDateTime receivedDateTime) {
        this.caseId = caseId;
        this.material = material;
        this.prosecutingAuthority = prosecutingAuthority;
        this.prosecutorDefendantId = prosecutorDefendantId;
        this.receivedDateTime = receivedDateTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Material getMaterial() {
        return material;
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
        return new DocumentUnbundled.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseId, material, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
    }

    @Override
    public String toString() {
        return "DocumentUnbundled{" +
                "caseId='" + caseId + "'," +
                "material='" + material + "'," +
                "prosecutingAuthority='" + prosecutingAuthority + "'," +
                "prosecutorDefendantId='" + prosecutorDefendantId + "'," +
                "receivedDateTime='" + receivedDateTime + "'" +
                "}";
    }

    public static class Builder {
        private UUID caseId;

        private Material material;

        private Optional<String> prosecutingAuthority;

        private String prosecutorDefendantId;

        private ZonedDateTime receivedDateTime;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withMaterial(final Material material) {
            this.material = material;
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

        public DocumentUnbundled build() {
            return new DocumentUnbundled(caseId, material, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
        }
    }
}
