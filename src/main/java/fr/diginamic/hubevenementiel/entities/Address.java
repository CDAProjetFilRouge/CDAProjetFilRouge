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
    private String street1;
    @Column(name = "address_line2")
    private String street2;
    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @OneToMany(mappedBy = "address")
    private List<Club> clubs = new ArrayList<>();

    @OneToMany(mappedBy = "address")
    private List<AppUser> users = new ArrayList<>();

    @OneToMany(mappedBy = "location")
    private List<Event> events = new ArrayList<>();

    public Address() {
    }

    public Address(Long id, String street1, String street2, String postalCode, String city, String country,
            List<Club> clubs, List<AppUser> users, List<Event> events) {
        this.id = id;
        this.street1 = street1;
        this.street2 = street2;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.clubs = clubs;
        this.users = users;
        this.events = events;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
