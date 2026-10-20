package fr.diginamic.hubevenementiel.dtos.appUser;

import java.util.UUID;
import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.enums.Role;

import java.util.List;

public class AppUserAdminUpdateRequestDto {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressRequestDto address;
    private Role role;
    private List<UUID> clubIds;

    public AppUserAdminUpdateRequestDto() {
    }

    public AppUserAdminUpdateRequestDto(String firstName, String lastName, String email, String phone,
            AddressRequestDto address, Role role, List<UUID> clubIds) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
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

    public AddressRequestDto getAddress() {
        return address;
    }

    public void setAddress(AddressRequestDto address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<UUID> getClubIds() {
        return clubIds;
    }

    public void setClubIds(List<UUID> clubIds) {
        this.clubIds = clubIds;
    }
}
