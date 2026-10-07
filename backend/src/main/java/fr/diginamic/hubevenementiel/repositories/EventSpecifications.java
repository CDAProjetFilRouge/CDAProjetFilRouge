package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Specification<Event> = condition WHERE construite en Java, combinable avec .and(...).
public class EventSpecifications {

    private EventSpecifications() {
    }

    // RG14/RG15 : un brouillon n'est visible que par son organisateur ou un admin.
    public static Specification<Event> visibleTo(AppUserPrincipal principal) {
        return (root, query, cb) -> {
            if(principal == null) {
                return cb.notEqual(root.get("status"), EventStatus.DRAFT);
            }

            if ("ADMINISTRATOR".equals(principal.role())) {
                return cb.conjunction(); // toujours vrai
            }

            Predicate notDraft = cb.notEqual(root.get("status"), EventStatus.DRAFT);
            Predicate ownDraft = cb.and(
                    cb.equal(root.get("status"), EventStatus.DRAFT),
                    cb.equal(root.get("organizer").get("id"), principal.id()));

            return cb.or(notDraft, ownDraft);
        };
    }

    // null = filtre non demande, Spring Data l'ignore dans un .and(...).
    public static Specification<Event> hasCategory(Category category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Event> startsOnOrAfter(LocalDateTime startDate) {
        if (startDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDateTime"), startDate);
    }

    public static Specification<Event> endsOnOrBefore(LocalDateTime endDate) {
        if (endDate == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDateTime"), endDate);
    }

    public static Specification<Event> nonAffiliatePriceAtLeast(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("nonAffiliatePrice"), minPrice);
    }

    public static Specification<Event> nonAffiliatePriceAtMost(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("nonAffiliatePrice"), maxPrice);
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Event> titleOrDescription(String keyword){
        if(keyword == null || keyword.isBlank()){
            return null;
        }

        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";

            Predicate titleContains = cb.like(cb.lower(root.get("title")), pattern);
            Predicate descriptionContains = cb.like(cb.lower(root.get("description")), pattern);

            return cb.or(titleContains, descriptionContains);
        };
    }

    public static Specification<Event> hasCity(String city){
        if(city == null || city.isBlank()){
            return null;
        }
        return (root, query, cb) -> {
            Join<Event, Address> location = root.join("location");

            return cb.equal(location.get("city"), city);
        };
    }

    public static Specification<Event> organizerFirstName(String organizerFirstName){
        if(organizerFirstName == null|| organizerFirstName.isBlank()){
            return null;
        }

        return (root, query, cb) -> {
            Join<Event, AppUser> organizer = root.join("organizer");

            return cb.and(
                    cb.equal(organizer.get("firstName"), organizerFirstName ),
                    cb.equal(organizer.get("role"), Role.ORGANIZER)
            );
        };
    }

}
