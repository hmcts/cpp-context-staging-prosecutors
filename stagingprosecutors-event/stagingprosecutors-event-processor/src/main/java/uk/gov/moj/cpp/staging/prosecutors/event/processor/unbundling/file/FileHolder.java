package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.file;

import java.util.UUID;

public class FileHolder {

    private UUID fileStoreId;
    private String sectionName;

    public FileHolder(final UUID fileStoreId, final String sectionName) {
        this.fileStoreId = fileStoreId;
        this.sectionName = sectionName;
    }

    public UUID getFileStoreId() {
        return fileStoreId;
    }

    public String getSectionName() {
        return sectionName;
    }
}
