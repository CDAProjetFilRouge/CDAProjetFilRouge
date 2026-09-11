package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepo extends CrudRepository<User, Long> {
}
