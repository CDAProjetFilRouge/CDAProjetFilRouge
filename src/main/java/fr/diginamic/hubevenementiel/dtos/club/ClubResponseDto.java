package fr.diginamic.hubevenementiel.dtos.club;

import fr.diginamic.hubevenementiel.dtos.address.AddressResponseDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClubResponseDto {

    private Long id;
    private String name;
    private Category category;
    private String email;
    private String phone;
    private LocalDate endValidityDate;
    private AddressResponseDto address;
    private List<AppUserSummaryResponseDto> appUsers = new ArrayList<>();

    public ClubResponseDto() {
    }

    public ClubResponseDto(Long id, String name, Category category, String email, String phone, LocalDate endValidityDate, AddressResponseDto address, List<AppUserSummaryResponseDto> appUsers) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.email = email;
        this.phone = phone;
        this.endValidityDate = endValidityDate;
        this.address = address;
        this.appUsers = appUsers;
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

    public AddressResponseDto getAddress() {
        return address;
    }

    public void setAddress(AddressResponseDto address) {
        this.address = address;
    }

    public List<AppUserSummaryResponseDto> getAppUsers() {
        return appUsers;
    }

    public void setAppUsers(List<AppUserSummaryResponseDto> appUsers) {
        this.appUsers = appUsers;
    }
}
