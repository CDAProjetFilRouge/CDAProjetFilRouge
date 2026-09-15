package fr.diginamic.hubevenementiel.dtos.appUser;

import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;

public class AppUserUpdateRequestDto {

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private AddressRequestDto address;

    public AppUserUpdateRequestDto() {
    }

    public AppUserUpdateRequestDto(String firstName, String lastName, String email, String phoneNumber, AddressRequestDto address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AddressRequestDto getAddress() {
        return address;
    }

    public void setAddress(AddressRequestDto address) {
        this.address = address;
    }
}
