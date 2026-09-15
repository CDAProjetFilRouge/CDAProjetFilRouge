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

    public List<Comment> findByEvent(Long eventId, int page, int size) throws HttpException {
        eventService.findById(eventId);

        Pageable pageable = PageRequest.of(page, size);
        return commentRepo.findByEventId(eventId, pageable).getContent();
    }

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

    @Transactional
    public void deleteComment(Long id) throws HttpException {
        Comment comment = findCommentById(id);

        commentRepo.delete(comment);
    }

    public List<Comment> getAllComment() {
        return commentRepo.findAll();
    }

    public Comment findCommentById(Long id) throws HttpException {
        Optional<Comment> c = commentRepo.findById(id);
        if (c.isEmpty()) {
            throw new NotFoundException("No comment found with id: " + id);
        }

        return c.get();
    }

    public List<Comment> findByAuthorId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByAuthorId(id, pageable).getContent();

        if (comments.isEmpty()) {
            throw new NotFoundException("No comments found for author with id: " + id);
        }

        return comments;
    }

    public List<Comment> findByEventId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> comments = commentRepo.findByEventId(id, pageable).getContent();

        if (comments.isEmpty()) {
            throw new NotFoundException("No comments found with event id: " + id);
        }

        return comments;
    }

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
