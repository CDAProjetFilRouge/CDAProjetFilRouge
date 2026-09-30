package fr.diginamic.hubevenementiel.dtos.club;

import fr.diginamic.hubevenementiel.enums.Category;


public class ClubSummaryResponseDto {

    private Long id;
    private String name;
    private Category category;

    public ClubSummaryResponseDto() {
    }

    public ClubSummaryResponseDto(Long id, String name, Category category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
