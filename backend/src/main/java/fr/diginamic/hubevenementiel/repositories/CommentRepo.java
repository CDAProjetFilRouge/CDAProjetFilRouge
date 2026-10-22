package fr.diginamic.hubevenementiel.repositories;

import java.util.UUID;
import fr.diginamic.hubevenementiel.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface CommentRepo extends JpaRepository<Comment, UUID> {

    /**
     *
     * @param id id of the author you want to find all comments associated with it
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByAuthorId(UUID id, Pageable pageable);

    /**
     *
     * @param id if od the event you want to find all the comments asscociated with it
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByEventId(UUID id, Pageable pageable);

    long countByEventIdAndAuthorId(UUID eventId, UUID authorId);

    boolean existsByEventIdAndAuthorIdAndContentIgnoreCase(UUID eventId, UUID authorId, String content);

    /**
     *
     * @param dateMin starting date of the comment you want to do the search on
     * @param dateMax maximal date of the comment you want to do the serach on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of comment with pagination info
     */
    Page<Comment> findByCreationDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param authorId id of the author
     * @return true if the user posted at least one comment
     */
    boolean existsByAuthorId(UUID authorId);

    /**
     *
     * @param authorId id of the author whose comments must be removed
     */
    void deleteByAuthorId(UUID authorId);
}
