package fr.diginamic.hubevenementiel.entities;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Entity
@Table(name = "image")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "path", nullable = false, length = 500)
    private String path;
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    @Column(name = "size_byte", nullable = false, length = 20)
    private long sizeByte;
    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;
    @Column(name = "display_order", nullable = false, length = 10)
    private int displayOrder;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    public Image() {
    }


    public Image(Long id, String fileName, String path, String mimeType, long sizeByte, LocalDateTime uploadDate, int displayOrder) {
        this.id = id;
        this.fileName = fileName;
        this.path = path;
        this.mimeType = mimeType;
        this.sizeByte = sizeByte;
        this.uploadDate = uploadDate;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeByte() {
        return sizeByte;
    }

    public void setSizeByte(long sizeByte) {
        this.sizeByte = sizeByte;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
