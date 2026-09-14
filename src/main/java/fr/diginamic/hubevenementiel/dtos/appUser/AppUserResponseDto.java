package fr.diginamic.hubevenementiel.dtos.appUser;

import fr.diginamic.hubevenementiel.dtos.address.AddressResponseDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppUserResponseDto {

    private Long id;
    private String lastName;
    private String firstName;
    private String email;
    private String phone;
    private Role role;
    private AccountStatus status;
    private LocalDateTime suspensionEndDate;
    private LocalDate creationDate;
    private AddressResponseDto address;
    private List<ClubSummaryResponseDto> clubs = new ArrayList<>();

    public AppUserResponseDto() {
    }

    public AppUserResponseDto(Long id, String lastName, String firstName, String email, String phone, Role role, AccountStatus status, LocalDateTime suspensionEndDate, LocalDate creationDate, AddressResponseDto address, List<ClubSummaryResponseDto> clubs) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.suspensionEndDate = suspensionEndDate;
        this.creationDate = creationDate;
        this.address = address;
        this.clubs = clubs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
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

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getSuspensionEndDate() {
        return suspensionEndDate;
    }

    public void setSuspensionEndDate(LocalDateTime suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public AddressResponseDto getAddress() {
        return address;
    }

    public void setAddress(AddressResponseDto address) {
        this.address = address;
    }

    public List<ClubSummaryResponseDto> getClubs() {
        return clubs;
    }

    public void setClubs(List<ClubSummaryResponseDto> clubs) {
        this.clubs = clubs;
    }
}
