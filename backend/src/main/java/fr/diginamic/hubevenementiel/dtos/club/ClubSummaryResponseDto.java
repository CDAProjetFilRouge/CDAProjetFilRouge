package fr.diginamic.hubevenementiel.dtos.club;

import java.util.UUID;
import fr.diginamic.hubevenementiel.enums.Category;


public class ClubSummaryResponseDto {

    private UUID id;
    private String name;
    private Category category;

    public ClubSummaryResponseDto() {
    }

    public ClubSummaryResponseDto(UUID id, String name, Category category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
