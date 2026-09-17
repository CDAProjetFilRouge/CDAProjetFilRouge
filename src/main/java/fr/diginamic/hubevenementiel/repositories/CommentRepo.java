package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CommentRepo extends JpaRepository<Comment, Long> {

    /**
     *
     * @param id id of the author you want to find all comments associated with it
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByAuthorId(Long id, Pageable pageable);

    /**
     *
     * @param id if od the event you want to find all the comments asscociated with it
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByEventId(Long id, Pageable pageable);

    /**
     *
     * @param dateMin starting date of the comment you want to do the search on
     * @param dateMax maximal date of the comment you want to do the serach on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByCreationDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);
}
