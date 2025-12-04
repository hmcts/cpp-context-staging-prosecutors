package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo;

import uk.gov.moj.cpp.staging.prosecutors.domain.Material;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class DocumentBundleArrivedForUnbundling {
    private final UUID caseId;

    private final CmsDocumentIdentifier cmsDocumentIdentifier;

    private final Material material;

    private final String prosecutingAuthority;

    private final String prosecutorDefendantId;

    private final String defendantName;

    private final ZonedDateTime receivedDateTime;

    public DocumentBundleArrivedForUnbundling(final UUID caseId, final CmsDocumentIdentifier cmsDocumentIdentifier,
                                              final Material material, final String prosecutingAuthority, final String prosecutorDefendantId,
                                              final String defendantName, final ZonedDateTime receivedDateTime) {
        this.caseId = caseId;
        this.cmsDocumentIdentifier = cmsDocumentIdentifier;
        this.material = material;
        this.prosecutingAuthority = prosecutingAuthority;
        this.prosecutorDefendantId = prosecutorDefendantId;
        this.defendantName = defendantName;
        this.receivedDateTime = receivedDateTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CmsDocumentIdentifier getCmsDocumentIdentifier() {
        return cmsDocumentIdentifier;
    }

    public Material getMaterial() {
        return material;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getProsecutorDefendantId() {
        return prosecutorDefendantId;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public ZonedDateTime getReceivedDateTime() {
        return receivedDateTime;
    }

    public static Builder documentBundleArrivedForUnbundling() {
        return new DocumentBundleArrivedForUnbundling.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseId, cmsDocumentIdentifier, material, prosecutingAuthority, prosecutorDefendantId, receivedDateTime);
    }

    @Override
    public String toString() {
        return "DocumentBundleArrivedForUnbundling{" +
                "caseId='" + caseId + "'," +
                "cmsDocumentIdentifier='" + cmsDocumentIdentifier + "'," +
                "material='" + material + "'," +
                "prosecutingAuthority='" + prosecutingAuthority + "'," +
                "prosecutorDefendantId='" + prosecutorDefendantId + "'," +
                "defendantName='" + defendantName + "'," +
                "receivedDateTime='" + receivedDateTime + "'" +
                "}";
    }

    public static class Builder {
        private UUID caseId;

        private CmsDocumentIdentifier cmsDocumentIdentifier;

        private Material material;

        private String prosecutingAuthority;

        private String prosecutorDefendantId;

        private String defendantName;

        private ZonedDateTime receivedDateTime;

        public DocumentBundleArrivedForUnbundling.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withCmsDocumentIdentifier(final CmsDocumentIdentifier cmsDocumentIdentifier) {
            this.cmsDocumentIdentifier = cmsDocumentIdentifier;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withMaterial(final Material material) {
            this.material = material;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withProsecutingAuthority(final String prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withProsecutorDefendantId(final String prosecutorDefendantId) {
            this.prosecutorDefendantId = prosecutorDefendantId;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withDefendantName(final String defendantName) {
            this.defendantName = defendantName;
            return this;
        }

        public DocumentBundleArrivedForUnbundling.Builder withReceivedDateTime(final ZonedDateTime receivedDateTime) {
            this.receivedDateTime = receivedDateTime;
            return this;
        }

        public DocumentBundleArrivedForUnbundling build() {
            return new DocumentBundleArrivedForUnbundling(caseId, cmsDocumentIdentifier, material, prosecutingAuthority, prosecutorDefendantId, defendantName, receivedDateTime);
        }
    }
}
