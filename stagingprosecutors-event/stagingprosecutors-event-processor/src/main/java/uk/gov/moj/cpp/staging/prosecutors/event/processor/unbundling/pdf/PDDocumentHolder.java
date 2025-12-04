package uk.gov.moj.cpp.staging.prosecutors.event.processor.unbundling.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class PDDocumentHolder implements Closeable {

    private UUID id = UUID.randomUUID();
    private String documentName;
    private PDDocument pdDocument;
    private String sectionName;

    public PDDocumentHolder(final String documentName, final PDDocument pdDocument, final String sectionName) {
        requireNonNull(documentName);
        requireNonNull(pdDocument);
        requireNonNull(sectionName);

        this.documentName = documentName;
        this.pdDocument = pdDocument;
        this.sectionName = sectionName;
    }

    public UUID getId() {
        return id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getBookmarkTitle() {
        return pdDocument.getDocumentCatalog()
                .getDocumentOutline().getFirstChild().getTitle();
    }

    public PDPageTree getPages() {
        return pdDocument.getPages();
    }

    public void saveDocument(OutputStream outputStream) throws IOException {
        pdDocument.save(outputStream);
    }

    public void saveDocument(String fileName) throws IOException {
        pdDocument.save(fileName);
    }

    @Override
    public void close() throws IOException {
        pdDocument.close();
    }
}