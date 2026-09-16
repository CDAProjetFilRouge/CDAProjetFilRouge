package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepo extends JpaRepository<Token, Long> {
}
