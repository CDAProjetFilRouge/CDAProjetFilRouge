package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.User;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Page<User> findByLastName(String lastName, Pageable pageable);

    Page<User> findByFirstName(String firstName, Pageable pageable);

    Optional<User> findByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    Page<User> findBySuspensionEndDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    Page<User> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, Pageable pageable);
}
