package fr.diginamic.hubevenementiel.dtos.image;

import java.util.UUID;

public class ImageSummaryResponseDto {

    private UUID id;
    private String path;
    private int displayOrder;

    public ImageSummaryResponseDto() {
    }

    public ImageSummaryResponseDto(UUID id, String path, int displayOrder) {
        this.id = id;
        this.path = path;
        this.displayOrder = displayOrder;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
