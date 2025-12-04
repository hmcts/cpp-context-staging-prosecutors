package uk.gov.moj.cpp.staging.prosecutors.domain;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Material;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class RecordDocumentUnbundleResult {
    private final UUID caseId;

    private final Optional<String> errorMessage;

    private final Optional<Material> material;

    private final Optional<String> prosecutingAuthority;

    private final String prosecutorDefendantId;

    private final Optional<ZonedDateTime> receivedDateTime;

    public RecordDocumentUnbundleResult(final UUID caseId, final Optional<String> errorMessage, final Optional<Material> material, final Optional<String> prosecutingAuthority, final String prosecutorDefendantId, final Optional<ZonedDateTime> receivedDateTime) {
        this.caseId = caseId;
        this.errorMessage = errorMessage;
        this.material = material;
        this.prosecutingAuthority = prosecutingAuthority;
        this.prosecutorDefendantId = prosecutorDefendantId;
        this.receivedDateTime = receivedDateTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public Optional<Material> getMaterial() {
        return material;
    }

    public Optional<String> getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getProsecutorDefendantId() {
        return prosecutorDefendantId;
    }

    public Optional<ZonedDateTime> getReceivedDateTime() {
        return receivedDateTime;
    }

    public static Builder recordDocumentUnbundleResult() {
        return new RecordDocumentUnbundleResult.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseId, errorMessage, material, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
    }

    @Override
    public String toString() {
        return "RecordDocumentUnbundleResult{" +
                "caseId='" + caseId + "'," +
                "errorMessage='" + errorMessage + "'," +
                "material='" + material + "'," +
                "prosecutingAuthority='" + prosecutingAuthority + "'," +
                "prosecutorDefendantId='" + prosecutorDefendantId + "'," +
                "receivedDateTime='" + receivedDateTime + "'" +
                "}";
    }

    public static class Builder {
        private UUID caseId;

        private Optional<String> errorMessage;

        private Optional<Material> material;

        private Optional<String> prosecutingAuthority;

        private String prosecutorDefendantId;

        private Optional<ZonedDateTime> receivedDateTime;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withErrorMessage(final Optional<String> errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder withMaterial(final Optional<Material> material) {
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

        public Builder withReceivedDateTime(final Optional<ZonedDateTime> receivedDateTime) {
            this.receivedDateTime = receivedDateTime;
            return this;
        }

        public RecordDocumentUnbundleResult build() {
            return new RecordDocumentUnbundleResult(caseId, errorMessage, material, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
        }
    }
}
