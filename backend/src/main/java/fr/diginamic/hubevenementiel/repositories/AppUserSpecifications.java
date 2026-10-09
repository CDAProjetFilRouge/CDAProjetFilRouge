package fr.diginamic.hubevenementiel.repositories;

import org.springframework.data.jpa.domain.Specification;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;

public class AppUserSpecifications {

    public static Specification<AppUser> matchesSearch(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pattern = "%" + escaped + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
            cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
            cb.like(cb.lower(root.get("email")), pattern, '\\'));
    }

    public static  Specification<AppUser> hasRole(Role role) {
        if (role == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<AppUser> hasStatus(AccountStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

}