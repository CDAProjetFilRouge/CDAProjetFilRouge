package fr.diginamic.hubevenementiel.dtos.legalDocument;

import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;

import java.time.LocalDateTime;

public class LegalDocumentResponseDto {

    private Long id;
    private DocumentType documentType;
    private String content;
    private int version;
    private LocalDateTime updateDate;
    private String pdfPath;
    private AppUserSummaryResponseDto user;

    public LegalDocumentResponseDto() {
    }

    public LegalDocumentResponseDto(Long id, DocumentType documentType, String content, int version, LocalDateTime updateDate, String pdfPath, AppUserSummaryResponseDto user) {
        this.id = id;
        this.documentType = documentType;
        this.content = content;
        this.version = version;
        this.updateDate = updateDate;
        this.pdfPath = pdfPath;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public AppUserSummaryResponseDto getUser() {
        return user;
    }

    public void setUser(AppUserSummaryResponseDto user) {
        this.user = user;
    }
}
