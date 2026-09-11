package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepo extends JpaRepository<Token, Long> {
}
