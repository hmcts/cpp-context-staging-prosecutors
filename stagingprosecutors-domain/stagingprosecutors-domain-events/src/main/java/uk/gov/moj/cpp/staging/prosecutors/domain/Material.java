package uk.gov.moj.cpp.staging.prosecutors.domain;


import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class Material {
    private final String documentType;

    private final UUID fileStoreId;

    private final String fileType;

    private final boolean isUnbundledDocument;

    public Material(final String documentType, final UUID fileStoreId, final String fileType, final boolean isUnbundledDocument) {
        this.documentType = documentType;
        this.fileStoreId = fileStoreId;
        this.fileType = fileType;
        this.isUnbundledDocument = isUnbundledDocument;
    }

    public String getDocumentType() {
        return documentType;
    }

    public UUID getFileStoreId() {
        return fileStoreId;
    }

    public String getFileType() {
        return fileType;
    }

    public boolean getIsUnbundledDocument() {
        return isUnbundledDocument;
    }

    public static Builder material() {
        return new Material.Builder();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(documentType, fileStoreId, fileType);
    }

    @Override
    public String toString() {
        return "Material{" +
                "documentType='" + documentType + "'," +
                "fileStoreId='" + fileStoreId + "'," +
                "fileType='" + fileType + "'," +
                "isUnbundledDocument='" + isUnbundledDocument + "'" +
                "}";
    }

    public static class Builder {
        private String documentType;

        private UUID fileStoreId;

        private String fileType;

        private boolean isUnbundledDocument;

        public Builder withDocumentType(final String documentType) {
            this.documentType = documentType;
            return this;
        }

        public Builder withFileStoreId(final UUID fileStoreId) {
            this.fileStoreId = fileStoreId;
            return this;
        }

        public Builder withFileType(final String fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder withIsUnbundledDocument(final boolean isUnbundledDocument) {
            this.isUnbundledDocument = isUnbundledDocument;
            return this;
        }

        public Material build() {
            return new Material(documentType, fileStoreId, fileType, isUnbundledDocument);
        }
    }
}