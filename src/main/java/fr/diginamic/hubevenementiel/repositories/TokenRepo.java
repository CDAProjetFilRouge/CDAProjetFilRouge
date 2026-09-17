package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepo extends JpaRepository<Token, Long> {

    Optional<Token> findByValue(String value);
}
