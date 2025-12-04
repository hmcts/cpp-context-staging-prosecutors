package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pojo;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class CmsDocumentIdentifier {
    private final String documentId;

    private final Integer materialType;

    public CmsDocumentIdentifier(final String documentId, final Integer materialType) {
        this.documentId = documentId;
        this.materialType = materialType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Integer getMaterialType() {
        return materialType;
    }

    public static Builder cmsDocumentIdentifier() {
        return new CmsDocumentIdentifier.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(documentId, materialType);
    }

    @Override
    public String toString() {
        return "CmsDocumentIdentifier{" +
                "documentId='" + documentId + "'," +
                "materialType='" + materialType + "'" +
                "}";
    }

    public static class Builder {
        private String documentId;

        private Integer materialType;

        public Builder withDocumentId(final String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder withMaterialType(final Integer materialType) {
            this.materialType = materialType;
            return this;
        }

        public CmsDocumentIdentifier build() {
            return new CmsDocumentIdentifier(documentId, materialType);
        }
    }
}
