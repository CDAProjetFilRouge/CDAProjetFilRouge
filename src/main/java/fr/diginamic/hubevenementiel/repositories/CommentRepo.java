package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;

public interface CommentRepo extends JpaRepository<Comment, Long> {

    /**
     *
     * @param id id of the author you want to find all comments of
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByAuthorId(Long id, Pageable pageable);

    /**
     *
     * @param id if od the event you want to find all the comments of
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByEventId(Long id, Pageable pageable);

    /**
     *
     * @param dateMin minimal date at which you want to find all comment of
     * @param dateMax maximal date at which you want to find all comment of
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByCreationDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);
}
