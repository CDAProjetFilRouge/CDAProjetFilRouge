package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Event;
import org.springframework.data.repository.CrudRepository;

public interface EventRepo extends CrudRepository<Event, Long> {
}
