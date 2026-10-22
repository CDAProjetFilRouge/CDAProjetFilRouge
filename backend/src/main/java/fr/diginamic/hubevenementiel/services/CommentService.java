package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.TooManyRequestsException;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private static final int MAX_COMMENTS_PER_EVENT = 20;
    private static final int MAX_COMMENTS_PER_WINDOW = 5;
    private static final long WINDOW_MILLIS = 60_000;

    private final CommentRepo commentRepo;
    private final AppUserService appUserService;
    private final EventService eventService;
    private final InscriptionRepo inscriptionRepo;
    private final RequestRateLimiter rateLimiter;
    private final CommentContentFilter contentFilter;

    public CommentService(CommentRepo commentRepo, AppUserService appUserService, EventService eventService,
            InscriptionRepo inscriptionRepo, RequestRateLimiter rateLimiter, CommentContentFilter contentFilter) {
        this.commentRepo = commentRepo;
        this.appUserService = appUserService;
        this.eventService = eventService;
        this.inscriptionRepo = inscriptionRepo;
        this.rateLimiter = rateLimiter;
        this.contentFilter = contentFilter;
    }

    /**
     *
     * @param eventId id of the event we want to get all the comment from
     * @param page starting page
     * @param size number of entries per page
     * @return a list of comments
     * @throws HttpException
     */
    public List<Comment> findByEvent(UUID eventId, int page, int size) throws HttpException {
        eventService.findById(eventId);

        Pageable pageable = PageRequest.of(page, size);
        return commentRepo.findByEventId(eventId, pageable).getContent();
    }

    /**
     *
     * @param eventId id of the event to associate the comment with
     * @param authorId id of the author to associate the comment with
     * @param content content of the comment
     * @return saved comment in the DB
     * @throws HttpException
     */
    @Transactional
    public Comment createComment(UUID eventId, UUID authorId, String content) throws HttpException {
        Event event = eventService.findById(eventId);
        AppUser author = appUserService.findById(authorId);

        contentFilter.check(content);
        checkCanPost(event, author);
        checkNotSpam(event, author, content);

        Comment comment = new Comment(author, event, content);
        comment.setCreationDate(LocalDateTime.now());

        return commentRepo.save(comment);
    }

    // Un évènement annulé n'est commenté que par son organisateur (ou un administrateur). Une fois terminé, seuls les
    // participants et l'organisateur commentent, sauf si l'organisateur a fermé les commentaires : plus personne alors.
    private void checkCanPost(Event event, AppUser author) throws HttpException {
        boolean privileged = author.getRole() == Role.ADMINISTRATOR
                || (event.getOrganizer() != null && event.getOrganizer().getId().equals(author.getId()));

        if (event.getStatus() == EventStatus.CANCELLED) {
            if (!privileged) {
                throw new ForbiddenException("Cet évènement a été annulé : les commentaires sont réservés à son organisateur.");
            }
            return;
        }

        boolean ended = event.getStatus() == EventStatus.FINISHED
                || (event.getEndDateTime() != null && !event.getEndDateTime().isAfter(LocalDateTime.now()));
        if (!ended) {
            return;
        }
        if (event.isCommentsClosedAfterEnd()) {
            throw new ForbiddenException("Les commentaires sont fermés pour cet évènement terminé.");
        }
        if (!privileged && !inscriptionRepo.existsByUserIdAndEventIdAndStatus(author.getId(), event.getId(),
                InscriptionStatus.CONFIRMED)) {
            throw new ForbiddenException("Seuls les participants peuvent commenter un évènement terminé.");
        }
    }

    private void checkNotSpam(Event event, AppUser author, String content) throws HttpException {
        if (commentRepo.existsByEventIdAndAuthorIdAndContentIgnoreCase(event.getId(), author.getId(), content.trim())) {
            throw new ConflictException("Vous avez déjà publié ce commentaire.");
        }
        if (commentRepo.countByEventIdAndAuthorId(event.getId(), author.getId()) >= MAX_COMMENTS_PER_EVENT) {
            throw new ConflictException("Vous avez atteint le nombre maximal de commentaires pour cet évènement.");
        }
        long retryAfterMillis = rateLimiter.tryConsume("comment:" + author.getId(), MAX_COMMENTS_PER_WINDOW,
                WINDOW_MILLIS);
        if (retryAfterMillis > 0) {
            long seconds = Math.max(1, (retryAfterMillis + 999) / 1000);
            throw new TooManyRequestsException("Vous publiez trop vite. Réessayez dans " + seconds + " s.", seconds);
        }
    }

    public void checkModificationRights(Comment comment, AppUserPrincipal connectedUser) throws HttpException {
        boolean isAuthor = comment.getAuthor().getId().equals(connectedUser.id());

        if (!isAuthor) {
            throw new ForbiddenException("Seul l'auteur peut modifier son commentaire.");
        }
    }

    public void checkDeleteRights(Comment comment, AppUserPrincipal connectedUser) throws HttpException {
        boolean isAdmin = "ADMINISTRATOR".equals(connectedUser.role());
        boolean isAuthor = comment.getAuthor().getId().equals(connectedUser.id());
        boolean isOrganizer = comment.getEvent().getOrganizer().getId().equals(connectedUser.id());

        if (!(isAdmin || isAuthor || isOrganizer)) {
            throw new ForbiddenException("Vous n'avez pas le droit de supprimer ce commentaire.");
        }
    }

    @Transactional
    public Comment updateComment(UUID eventId, UUID commentId, String newComment, AppUserPrincipal principal) throws HttpException {
        Comment comment = findCommentById(commentId);

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new NotFoundException("Commentaire introuvable.");
        }

        checkModificationRights(comment, principal);
        contentFilter.check(newComment);

        comment.setContent(newComment);
        return commentRepo.save(comment);
    }

    /**
     *
     * @param eventId id of the event the comment must belong to
     * @param commentId id of the comment to delete
     * @param principal the authenticated caller, must be the author, the event's organizer or an admin
     * @throws HttpException
     */
    @Transactional
    public void deleteComment(UUID eventId, UUID commentId, AppUserPrincipal principal) throws HttpException {
        Comment comment = findCommentById(commentId);

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new NotFoundException("Commentaire introuvable.");
        }

        checkDeleteRights(comment, principal);

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
    public Comment findCommentById(UUID id) throws HttpException {
        Optional<Comment> c = commentRepo.findById(id);
        if (c.isEmpty()) {
            throw new NotFoundException("Aucun commentaire trouvé avec l'id : " + id);
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
    public List<Comment> findByAuthorId(UUID id, int page, int size) throws HttpException {
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
    public List<Comment> findByEventId(UUID id, int page, int size) throws HttpException {
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
