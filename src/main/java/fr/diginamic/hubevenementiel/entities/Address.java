package fr.diginamic.hubevenementiel.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "address_line1", nullable = false)
    private String Street1;
    @Column(name = "address_line2")
    private String Street2;
    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;
    @Column(name = "city", nullable = false, length = 100)
    private String City;
    @Column(name = "country", nullable = false, length = 100)
    private String Country;

    @OneToMany(mappedBy = "address")
    private List<Club> clubs = new ArrayList<>();

    @OneToMany(mappedBy = "address")
    private List<AppUser> users = new ArrayList<>();

    public Address() {
    }

    public Address(Long id, String street1, String street2, String postalCode, String city, String country,
            List<Club> clubs, List<AppUser> users) {
        this.id = id;
        Street1 = street1;
        Street2 = street2;
        this.postalCode = postalCode;
        City = city;
        Country = country;
        this.clubs = clubs;
        this.users = users;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet1() {
        return Street1;
    }

    public void setStreet1(String street1) {
        Street1 = street1;
    }

    public String getStreet2() {
        return Street2;
    }

    public void setStreet2(String street2) {
        Street2 = street2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }

    public List<Club> getClubs() {
        return clubs;
    }

    public void setClubs(List<Club> clubs) {
        this.clubs = clubs;
    }

    public List<AppUser> getUsers() {
        return users;
    }

    public void setUsers(List<AppUser> users) {
        this.users = users;
    }

}
