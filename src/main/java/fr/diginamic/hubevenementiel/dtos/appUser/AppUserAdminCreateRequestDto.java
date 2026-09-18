package fr.diginamic.hubevenementiel.dtos.appUser;

import fr.diginamic.hubevenementiel.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AppUserAdminCreateRequestDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotNull
    private Role role;

    private List<Long> clubIds;

    public AppUserAdminCreateRequestDto() {
    }

    public AppUserAdminCreateRequestDto(String firstName, String lastName, String email, String phone, Role role, List<Long> clubIds) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.clubIds = clubIds;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Long> getClubIds() {
        return clubIds;
    }

    public void setClubIds(List<Long> clubIds) {
        this.clubIds = clubIds;
    }

}
