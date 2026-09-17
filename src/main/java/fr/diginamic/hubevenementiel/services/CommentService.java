package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepo commentRepo;
    private final AppUserService appUserService;
    private final EventService eventService;

    public CommentService(CommentRepo commentRepo, AppUserService appUserService, EventService eventService) {
        this.commentRepo = commentRepo;
        this.appUserService = appUserService;
        this.eventService = eventService;
    }

    /**
     *
     * @param eventId id of the event we want to get all the comment from
     * @param page starting page
     * @param size number of entries per page
     * @return a list of comments
     * @throws HttpException
     */
    public List<Comment> findByEvent(Long eventId, int page, int size) throws HttpException {
        eventService.findById(eventId);

        Pageable pageable = PageRequest.of(page, size);
        return commentRepo.findByEventId(eventId, pageable).getContent();
    }

    /**
     *
     * @param eventId id of the event to associate the comment with
     * @param authorId id of the author to associate the comment with
     * @param content content of the comment
     * @return savec comment in the DB
     * @throws HttpException
     */
    @Transactional
    public Comment createComment(Long eventId, Long authorId, String content) throws HttpException {
        Event event = eventService.findById(eventId);
        AppUser author = appUserService.findById(authorId);

        if (content == null || content.isBlank()) {
            throw new BadRequestException("Le commentaire ne peut pas être vide.");
        }

        Comment comment = new Comment(author, event, content);
        comment.setCreationDate(LocalDateTime.now());

        return commentRepo.save(comment);
    }

    /**
     *
     * @param id id of the comment to delete
     * @throws HttpException
     */
    @Transactional
    public void deleteComment(Long id) throws HttpException {
        Comment comment = findCommentById(id);

        commentRepo.delete(comment);
    }

    /**
     *
     * @return return a list of comments
     */
    public List<Comment> getAllComment() {
        return commentRepo.findAll();
    }

    /**
     *
     * @param id id of the comment to find
     * @return object of type Comment
     * @throws HttpException
     */
    public Comment findCommentById(Long id) throws HttpException {
        Optional<Comment> c = commentRepo.findById(id);
        if (c.isEmpty()) {
            throw new NotFoundException("No comment found with id: " + id);
        }

        return c.get();
    }

    /**
     *
     * @param id id of the author to find all comments associated with
     * @param page starting page
     * @param size number of entries per page
     * @return a list of comments
     * @throws HttpException
     */
    public List<Comment> findByAuthorId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByAuthorId(id, pageable).getContent();

        if (comments.isEmpty()) {
            throw new NotFoundException("No comments found for author with id: " + id);
        }

        return comments;
    }

    /**
     *
     * @param id id of the event to find all comments associated with
     * @param page starting page
     * @param size number of entries per page
     * @return a list of comments
     * @throws HttpException
     */
    public List<Comment> findByEventId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByEventId(id, pageable).getContent();

        if (comments.isEmpty()) {
            throw new NotFoundException("No comments found with event id: " + id);
        }

        return comments;
    }

    /**
     *
     * @param dateMin starting date to do the search on
     * @param dateMax maximum date to do the search on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of comments
     * @throws HttpException
     */
    public List<Comment> findByCreationDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByCreationDateBetween(dateMin, dateMax, pageable).getContent();

        if (comments.isEmpty()) {
            throw new NotFoundException("Not comment found between " + dateMin + " and " + dateMax);
        }

        return comments;
    }

}
