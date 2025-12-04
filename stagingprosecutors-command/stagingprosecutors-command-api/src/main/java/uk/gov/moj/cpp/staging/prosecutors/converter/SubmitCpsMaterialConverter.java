package uk.gov.moj.cpp.staging.prosecutors.converter;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDefendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.CpsDocument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import com.cgi.cp.cp20._2020_03.CP20;
import com.cgi.cp.cp20._2020_03.TDefendant;
import com.cgi.cp.cp20._2020_03.TDocument;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.SubmitCpsMaterialCommand;

public class SubmitCpsMaterialConverter {

    public SubmitCpsMaterialCommand convert(final CP20 cp20, final List<UUID> fileStoreIds) {
        return SubmitCpsMaterialCommand.submitCpsMaterialCommand()
                .withCompassCaseId(cp20.getCompassCaseId())
                .withResponseEmail(cp20.getResponseEmail())
                .withTransactionID(cp20.getTransactionID())
                .withSubmissionId(randomUUID())
                .withUrn(cp20.getURN())
                .withDefendants(getDefendantList(cp20))
                .withDocuments(getDocuments(cp20, fileStoreIds))
                .build();
    }

    private List<CpsDocument> getDocuments(final CP20 cp20, final List<UUID> fileStoreIds) {
        final List<CpsDocument> cpsDocumentList = new ArrayList<>();
        final List<TDocument> documentList = cp20.getDocuments().getDocument();
        if (documentList != null) {
            for(int i = 0; i < documentList.size(); i++){
                cpsDocumentList.add(getDocument(documentList.get(i), fileStoreIds.get(i)));
            }
        }
        return cpsDocumentList;
    }

    private CpsDocument getDocument(final TDocument tDocument, final UUID fileStoreId) {
        return CpsDocument.cpsDocument()
                .withDocumentId(tDocument.getDocumentId())
                .withFileName(tDocument.getFileName())
                .withMaterialType(tDocument.getMaterialType())
                .withFileStoreId(fileStoreId)
                .build();
    }


    private List<CpsDefendant> getDefendantList(final CP20 cp20) {

        return cp20.getDefendants().getDefendant().stream().map(this::getDefendant).collect(toList());

    }

    private CpsDefendant getDefendant(final TDefendant tDefendant) {
        return CpsDefendant.cpsDefendant()
                .withAsn(tDefendant.getASN())
                .withDefendantID(tDefendant.getCMSDefendantID().toString())
                .withDob(tDefendant.getDOB() == null ? null : getDOB(tDefendant.getDOB()))
                .withForenames(tDefendant.getForenames())
                .withSurname(tDefendant.getSurname())
                .withOuCode(tDefendant.getOUCode())
                .build();

    }

    private LocalDate getDOB(final XMLGregorianCalendar xmlGregorianCalendar) {
        return LocalDate.of(
                xmlGregorianCalendar.getYear(),
                xmlGregorianCalendar.getMonth(),
                xmlGregorianCalendar.getDay());
    }


}
