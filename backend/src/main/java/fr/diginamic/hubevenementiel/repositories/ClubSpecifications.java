package fr.diginamic.hubevenementiel.repositories;

import org.springframework.data.jpa.domain.Specification;

import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;

public class ClubSpecifications {


    public static Specification<Club> matchNames(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("name")), pattern));
    }

    public static Specification<Club> hasCategory(Category category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);  
    }

    public static Specification<Club> inCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String pattern = "%" + city.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("address").get("city")), pattern));
    }
}
