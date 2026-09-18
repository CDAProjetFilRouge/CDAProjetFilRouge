package fr.diginamic.hubevenementiel.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "APP_USER")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lastname", length = 100)
    private String lastName;

    @Column(name = "firstname", length = 100)
    private String firstName;

    @Column(name = "email")
    private String email;

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "suspension_end_date")
    private LocalDateTime suspensionEndDate;

    @Column(name = "creatio_date")
    private LocalDate creationDate;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "requester")
    @JsonIgnore
    private List<AnonymizationDemand> requesters;

    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    private List<AnonymizationDemand> admins;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<LegalDocument> legalDocumentList;

    @ManyToMany

    private List<Club> clubs = new ArrayList<>();

    public AppUser() {
    };

    public AppUser(LocalDate creationDate, LocalDateTime suspensionEndDate, AccountStatus status, Role role,
            String phone,
            String hashedPassword, String email, String firstName, String lastName, Long id) {
        this.creationDate = creationDate;
        this.suspensionEndDate = suspensionEndDate;
        this.status = status;
        this.role = role;
        this.phone = phone;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = id;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getSuspensionEndDate() {
        return suspensionEndDate;
    }

    public void setSuspensionEndDate(LocalDateTime suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<AnonymizationDemand> getRequesters() {
        return requesters;
    }

    public void setRequesters(List<AnonymizationDemand> requesters) {
        this.requesters = requesters;
    }

    public List<AnonymizationDemand> getAdmins() {
        return admins;
    }

    public void setAdmins(List<AnonymizationDemand> admins) {
        this.admins = admins;
    }

    public List<LegalDocument> getLegalDocumentList() {
        return legalDocumentList;
    }

    public void setLegalDocumentList(List<LegalDocument> legalDocumentList) {
        this.legalDocumentList = legalDocumentList;
    }

    public List<Club> getClubs() {
        return clubs;
    }

    public void setClubs(List<Club> clubs) {
        this.clubs = clubs;
    }
}
