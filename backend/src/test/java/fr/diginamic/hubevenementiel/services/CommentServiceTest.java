package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Comment;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.TooManyRequestsException;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Mock
    private InscriptionRepo inscriptionRepo;
    @Mock
    private RequestRateLimiter rateLimiter;
    @Spy
    private CommentContentFilter contentFilter = new CommentContentFilter();

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
    // createComment : qui peut commenter, anti-spam, filtre
    // ---------------------------------------------------------------

    private void givenEventAndAuthor() throws HttpException {
        when(eventService.findById(uuid(2))).thenReturn(event);
        when(appUserService.findById(uuid(6))).thenReturn(author);
    }

    private void givenEnded(boolean commentsClosed) {
        event.setStatus(EventStatus.FINISHED);
        event.setEndDateTime(LocalDateTime.now().minusDays(1));
        event.setCommentsClosedAfterEnd(commentsClosed);
    }

    @Test
    void createComment_cancelledEvent_isReservedToItsOrganizer() throws HttpException {
        event.setStatus(EventStatus.CANCELLED);
        givenEventAndAuthor();

        assertThrows(ForbiddenException.class, () -> commentService.createComment(uuid(2), uuid(6), "Dommage"));
        verify(commentRepo, never()).save(any(Comment.class));

        when(appUserService.findById(uuid(8))).thenReturn(organizer);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(commentService.createComment(uuid(2), uuid(8), "Évènement reporté, merci de votre compréhension")
                .getAuthor()).isEqualTo(organizer);
    }

    @Test
    void createComment_endedEvent_isReservedToParticipantsAndOrganizer() throws HttpException {
        givenEnded(false);
        givenEventAndAuthor();
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatus(uuid(6), uuid(2), InscriptionStatus.CONFIRMED))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () -> commentService.createComment(uuid(2), uuid(6), "Belle soirée"));

        when(inscriptionRepo.existsByUserIdAndEventIdAndStatus(uuid(6), uuid(2), InscriptionStatus.CONFIRMED))
                .thenReturn(true);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(commentService.createComment(uuid(2), uuid(6), "Belle soirée").getContent())
                .isEqualTo("Belle soirée");
    }

    @Test
    void createComment_endedEventWithCommentsClosed_refusesEveryone() throws HttpException {
        givenEnded(true);
        givenEventAndAuthor();
        when(appUserService.findById(uuid(8))).thenReturn(organizer);

        assertThrows(ForbiddenException.class, () -> commentService.createComment(uuid(2), uuid(6), "Belle soirée"));
        assertThrows(ForbiddenException.class, () -> commentService.createComment(uuid(2), uuid(8), "Merci à tous"));
        verify(commentRepo, never()).save(any(Comment.class));
    }

    @Test
    void createComment_notEndedEventWithCommentsClosedOption_staysOpen() throws HttpException {
        event.setStatus(EventStatus.PUBLISHED);
        event.setEndDateTime(LocalDateTime.now().plusDays(3));
        event.setCommentsClosedAfterEnd(true);
        givenEventAndAuthor();
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(commentService.createComment(uuid(2), uuid(6), "Hâte d'y être").getContent())
                .isEqualTo("Hâte d'y être");
    }

    @Test
    void createComment_administratorIsAllowedOnAnEndedEvent() throws HttpException {
        givenEnded(false);
        AppUser admin = new AppUser();
        admin.setId(uuid(99));
        admin.setRole(Role.ADMINISTRATOR);
        when(eventService.findById(uuid(2))).thenReturn(event);
        when(appUserService.findById(uuid(99))).thenReturn(admin);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(commentService.createComment(uuid(2), uuid(99), "Rappel : photos à venir").getAuthor())
                .isEqualTo(admin);
    }

    @Test
    void createComment_duplicateOrTooMany_isRefused() throws HttpException {
        givenEventAndAuthor();
        when(commentRepo.existsByEventIdAndAuthorIdAndContentIgnoreCase(uuid(2), uuid(6), "Super")).thenReturn(true);
        assertThrows(ConflictException.class, () -> commentService.createComment(uuid(2), uuid(6), "  Super "));

        when(commentRepo.countByEventIdAndAuthorId(uuid(2), uuid(6))).thenReturn(20L);
        assertThrows(ConflictException.class, () -> commentService.createComment(uuid(2), uuid(6), "Un autre"));
        verify(commentRepo, never()).save(any(Comment.class));
    }

    @Test
    void createComment_tooFast_throwsTooManyRequests() throws HttpException {
        givenEventAndAuthor();
        when(rateLimiter.tryConsume(org.mockito.ArgumentMatchers.eq("comment:" + uuid(6)),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(12_000L);

        assertThrows(TooManyRequestsException.class, () -> commentService.createComment(uuid(2), uuid(6), "Encore un"));
        verify(commentRepo, never()).save(any(Comment.class));
    }

    @Test
    void createComment_linkOrInsult_isRefusedBeforeAnyOtherCheck() throws HttpException {
        givenEventAndAuthor();

        assertThrows(BadRequestException.class, () -> commentService.createComment(uuid(2), uuid(6), "Voir www.promo.fr"));
        assertThrows(BadRequestException.class, () -> commentService.createComment(uuid(2), uuid(6), "Quel connard"));
        verify(commentRepo, never()).save(any(Comment.class));
        verify(rateLimiter, never()).tryConsume(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyLong());
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
