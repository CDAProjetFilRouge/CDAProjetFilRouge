package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Comment;
import org.springframework.data.repository.CrudRepository;

public interface CommentRepo extends CrudRepository<Comment, Long> {
}
