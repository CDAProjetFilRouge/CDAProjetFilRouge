package fr.diginamic.hubevenementiel.dtos.image;

public class ImageSummaryResponseDto {

    private Long id;
    private String path;
    private int displayOrder;

    public ImageSummaryResponseDto() {
    }

    public ImageSummaryResponseDto(Long id, String path, int displayOrder) {
        this.id = id;
        this.path = path;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
