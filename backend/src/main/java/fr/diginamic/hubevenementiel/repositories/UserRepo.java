package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepo extends JpaRepository<AppUser, Long> {

    /**
     *
     * @param lastName last name of the user you want to find
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findByLastName(String lastName, Pageable pageable);

    /**
     *
     * @param firstName first name of the user you want to find
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findByFirstName(String firstName, Pageable pageable);

    /**
     *
     * @param email email to find the user associated with
     * @return Optional of type user
     */
    Optional<AppUser> findByEmail(String email);

    /**
     *
     * @param role role you want to find user associated with
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findByRole(Role role, Pageable pageable);

    /**
     *
     * @param status status you want to find users associated with
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findByStatus(AccountStatus status, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which you want to find suspended users
     * @param dateMax maximum date at which you want to find suspended users
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findBySuspensionEndDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which you want to find users
     * @param dateMax maximum date at which you want to find users
     * @param pageable status you want to find inscription associated with
     * @return a list of users with pagination info
     */
    Page<AppUser> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, Pageable pageable);

    /**
     *
     * @param email email of which you want to find users associated with
     * @return true if user exist or false if not
     */
    boolean existsByEmail(String email);
}
