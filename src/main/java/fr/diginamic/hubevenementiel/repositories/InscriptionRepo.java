package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Inscription;
import org.springframework.data.repository.CrudRepository;

public interface InscriptionRepo extends CrudRepository<Inscription, Long> {
}
