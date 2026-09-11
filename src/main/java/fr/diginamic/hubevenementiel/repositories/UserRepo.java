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

    Page<AppUser> findByLastName(String lastName, Pageable pageable);

    Page<AppUser> findByFirstName(String firstName, Pageable pageable);

    Optional<AppUser> findByEmail(String email);

    Page<AppUser> findByRole(Role role, Pageable pageable);

    Page<AppUser> findByStatus(AccountStatus status, Pageable pageable);

    Page<AppUser> findBySuspensionEndDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    Page<AppUser> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, Pageable pageable);
}
