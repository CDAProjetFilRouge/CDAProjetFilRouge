package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepo extends JpaRepository<Token, Long> {

    Optional<Token> findByValue(String value);

    /**
     *
     * @param user user whose tokens must be removed
     */
    void deleteByUser(AppUser user);
}
