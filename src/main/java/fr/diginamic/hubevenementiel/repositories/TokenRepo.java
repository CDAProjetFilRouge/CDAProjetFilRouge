package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Token;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepo extends CrudRepository<Token, Long> {
}
