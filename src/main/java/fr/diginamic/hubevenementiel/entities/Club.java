package fr.diginamic.hubevenementiel.entities;

import fr.diginamic.hubevenementiel.enums.Category;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "club")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    @Column(name = "end_validity_date")
    private LocalDate endValidityDate;

    @ManyToOne
    @JoinColumn(name = "id_adress")
    private Adress adress;


    public Club() {
    }


    public Club(Long id, String name, Category category, Adress adress, String email, String phone, LocalDate endValidityDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.adress = adress;
        this.email = email;
        this.phone = phone;
        this.endValidityDate = endValidityDate;
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

    public Adress getAdress() {
        return adress;
    }

    public void setAdress(Adress adress) {
        this.adress = adress;
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


}
