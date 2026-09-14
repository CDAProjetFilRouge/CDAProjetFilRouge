package fr.diginamic.hubevenementiel.dtos.address;

public class AddressRequestDto {

    private String street1;
    private String street2;
    private String postalCode;
    private String city;
    private String country;

    public AddressRequestDto() {
    }

    public AddressRequestDto(String street1, String street2, String postalCode, String city, String country) {
        this.street1 = street1;
        this.street2 = street2;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
