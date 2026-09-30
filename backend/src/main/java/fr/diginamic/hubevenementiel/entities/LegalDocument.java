package fr.diginamic.hubevenementiel.entities;

import fr.diginamic.hubevenementiel.enums.DocumentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LEGAL_DOCUMENT")
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", length = 30)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(name = "content", length = 65535)
    private String content;

    @Column(name = "version")
    private int version;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private AppUser user;

    public LegalDocument(){};

    public LegalDocument(AppUser user, String pdfPath, LocalDateTime updateDate, String content, DocumentType documentType, long id, int version) {
        this.user = user;
        this.pdfPath = pdfPath;
        this.updateDate = updateDate;
        this.content = content;
        this.documentType = documentType;
        this.id = id;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
}
