package fr.diginamic.hubevenementiel.dtos.club;

import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.enums.Category;

import java.time.LocalDate;


public class ClubRequestDto {

    private String name;
    private Category category;
    private String email;
    private String phone;
    private LocalDate endValidityDate;
    private AddressRequestDto address;

    public ClubRequestDto() {
    }

    public ClubRequestDto(String name, Category category, String email, String phone, LocalDate endValidityDate, AddressRequestDto address) {
        this.name = name;
        this.category = category;
        this.email = email;
        this.phone = phone;
        this.endValidityDate = endValidityDate;
        this.address = address;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getEndValidityDate() {
        return endValidityDate;
    }

    public void setEndValidityDate(LocalDate endValidityDate) {
        this.endValidityDate = endValidityDate;
    }

    public AddressRequestDto getAddress() {
        return address;
    }

    public void setAddress(AddressRequestDto address) {
        this.address = address;
    }
}
