package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepo commentRepo;
    @Mock
    private AppUserService appUserService;
    @Mock
    private EventService eventService;

    @InjectMocks
    private CommentService commentService;

    private AppUser author;
    private AppUser organizer;
    private Event event;

    @BeforeEach
    void setUp() {
        author = new AppUser();
        author.setId(uuid(6));

        organizer = new AppUser();
        organizer.setId(uuid(8));

        event = new Event();
        event.setId(uuid(2));
        event.setOrganizer(organizer);
    }

    // ---------------------------------------------------------------
    // createComment
    // ---------------------------------------------------------------

    @Test
    void createComment_blankContent_throwsBadRequest() throws HttpException {
        when(eventService.findById(uuid(2))).thenReturn(event);
        when(appUserService.findById(uuid(6))).thenReturn(author);

        assertThrows(BadRequestException.class, () -> commentService.createComment(uuid(2), uuid(6), " "));
    }

    @Test
    void createComment_unknownEvent_throwsNotFound() throws HttpException {
        when(eventService.findById(uuid(999))).thenThrow(new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        assertThrows(NotFoundException.class, () -> commentService.createComment(uuid(999), uuid(6), "Super"));
    }

    @Test
    void createComment_authorDerivedFromGivenId() throws HttpException {
        when(eventService.findById(uuid(2))).thenReturn(event);
        when(appUserService.findById(uuid(6))).thenReturn(author);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.createComment(uuid(2), uuid(6), "Super évènement");

        assertThat(result.getAuthor()).isEqualTo(author);
    }

    // ---------------------------------------------------------------
    // checkModificationRights / updateComment
    // ---------------------------------------------------------------

    private Comment commentBy(AppUser commentAuthor) {
        Comment comment = new Comment();
        comment.setId(uuid(1));
        comment.setAuthor(commentAuthor);
        comment.setEvent(event);
        comment.setContent("Contenu original");
        return comment;
    }

    @Test
    void checkModificationRights_author_isAllowed() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> commentService.checkModificationRights(comment, principal));
    }

    @Test
    void checkModificationRights_organizer_isForbidden() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(8), "david@example.com", "ORGANIZER");

        assertThrows(ForbiddenException.class, () -> commentService.checkModificationRights(comment, principal));
    }

    @Test
    void checkModificationRights_admin_isForbidden() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(99), "emma@example.com", "ADMINISTRATOR");

        assertThrows(ForbiddenException.class, () -> commentService.checkModificationRights(comment, principal));
    }

    @Test
    void updateComment_wrongEventId_throwsNotFound() {
        Comment comment = commentBy(author);
        when(commentRepo.findById(uuid(1))).thenReturn(Optional.of(comment));
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        assertThrows(NotFoundException.class, () -> commentService.updateComment(uuid(999), uuid(1), "Nouveau", principal));
    }

    // ---------------------------------------------------------------
    // checkDeleteRights / deleteComment
    // ---------------------------------------------------------------

    @Test
    void checkDeleteRights_author_isAllowed() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> commentService.checkDeleteRights(comment, principal));
    }

    @Test
    void checkDeleteRights_eventOrganizer_isAllowed() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(8), "david@example.com", "ORGANIZER");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> commentService.checkDeleteRights(comment, principal));
    }

    @Test
    void checkDeleteRights_admin_isAllowed() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(99), "emma@example.com", "ADMINISTRATOR");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> commentService.checkDeleteRights(comment, principal));
    }

    @Test
    void checkDeleteRights_stranger_isForbidden() {
        Comment comment = commentBy(author);
        AppUserPrincipal principal = new AppUserPrincipal(uuid(42), "bob@example.com", "MEMBER");

        assertThrows(ForbiddenException.class, () -> commentService.checkDeleteRights(comment, principal));
    }

    @Test
    void deleteComment_wrongEventId_throwsNotFound() {
        Comment comment = commentBy(author);
        when(commentRepo.findById(uuid(1))).thenReturn(Optional.of(comment));
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        assertThrows(NotFoundException.class, () -> commentService.deleteComment(uuid(999), uuid(1), principal));
        verify(commentRepo, never()).delete(any());
    }

    @Test
    void deleteComment_authorizedCaller_deletesSuccessfully() throws HttpException {
        Comment comment = commentBy(author);
        when(commentRepo.findById(uuid(1))).thenReturn(Optional.of(comment));
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        commentService.deleteComment(uuid(2), uuid(1), principal);

        verify(commentRepo).delete(comment);
    }

    // ---------------------------------------------------------------
    // recherches
    // ---------------------------------------------------------------

    @Test
    void findByAuthorId_noResults_throwsNotFound() {
        when(commentRepo.findByAuthorId(eq(uuid(6)), any())).thenReturn(emptyPage());

        assertThrows(NotFoundException.class, () -> commentService.findByAuthorId(uuid(6), 0, 20));
    }

    @Test
    void findByEventId_noResults_throwsNotFound() {
        when(commentRepo.findByEventId(eq(uuid(2)), any())).thenReturn(emptyPage());

        assertThrows(NotFoundException.class, () -> commentService.findByEventId(uuid(2), 0, 20));
    }

    @Test
    void findByCreationDate_noResults_throwsNotFound() {
        when(commentRepo.findByCreationDateBetween(any(), any(), any())).thenReturn(emptyPage());

        assertThrows(NotFoundException.class,
                () -> commentService.findByCreationDate(LocalDateTime.now().minusDays(1), LocalDateTime.now(), 0, 20));
    }

    private static org.springframework.data.domain.Page<Comment> emptyPage() {
        return new org.springframework.data.domain.PageImpl<>(List.of());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
