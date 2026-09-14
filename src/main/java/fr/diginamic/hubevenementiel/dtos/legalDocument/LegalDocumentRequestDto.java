package fr.diginamic.hubevenementiel.dtos.legalDocument;

import fr.diginamic.hubevenementiel.enums.DocumentType;

public class LegalDocumentRequestDto {

    private DocumentType documentType;
    private String content;

    public LegalDocumentRequestDto() {
    }

    public LegalDocumentRequestDto(DocumentType documentType, String content) {
        this.documentType = documentType;
        this.content = content;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
