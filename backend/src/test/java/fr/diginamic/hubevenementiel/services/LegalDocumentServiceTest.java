package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock
    private LegalDocumentRepo legalDocumentRepo;

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    private LegalDocument validDocument;

    private AppUserPrincipal principal;

    private AppUser administrator;

    @BeforeEach
    void setUp() {
        principal = new AppUserPrincipal(1L, "admin@example.com", "ADMINISTRATOR");
        administrator = new AppUser();

        validDocument = new LegalDocument();
        validDocument.setDocumentType(DocumentType.TERM_OF_USE);
        validDocument.setContent("Conditions d'utilisation...");
    }

    // ---------------------------------------------------------------
    // legalDocumentChecker
    // ---------------------------------------------------------------

    @Test
    void legalDocumentChecker_nullDocument_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> legalDocumentService.legalDocumentChecker(null));
    }

    @Test
    void legalDocumentChecker_nullDocumentType_throwsBadRequest() {
        validDocument.setDocumentType(null);
        assertThrows(BadRequestException.class, () -> legalDocumentService.legalDocumentChecker(validDocument));
    }

    @Test
    void legalDocumentChecker_blankContent_throwsBadRequest() {
        validDocument.setContent(" ");
        assertThrows(BadRequestException.class, () -> legalDocumentService.legalDocumentChecker(validDocument));
    }

    @Test
    void legalDocumentChecker_nullContent_throwsBadRequest() {
        validDocument.setContent(null);
        assertThrows(BadRequestException.class, () -> legalDocumentService.legalDocumentChecker(validDocument));
    }

    // ---------------------------------------------------------------
    // createNewVersion
    // ---------------------------------------------------------------

    @Test
    void createNewVersion_firstVersionOfType_setsVersionOne() throws HttpException {
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.TERM_OF_USE))
                .thenReturn(Optional.empty());
        when(appUserService.findById(1L)).thenReturn(administrator);
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LegalDocument result = legalDocumentService.createNewVersion(validDocument, principal);

        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void createNewVersion_existingVersions_incrementsFromLatest() throws HttpException {
        LegalDocument previous = new LegalDocument();
        previous.setVersion(3);
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.TERM_OF_USE))
                .thenReturn(Optional.of(previous));
        when(appUserService.findById(1L)).thenReturn(administrator);
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LegalDocument result = legalDocumentService.createNewVersion(validDocument, principal);

        assertThat(result.getVersion()).isEqualTo(4);
    }

    @Test
    void createNewVersion_setsConnectedAdministratorAsAuthor() throws HttpException {
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.TERM_OF_USE))
                .thenReturn(Optional.empty());
        when(appUserService.findById(1L)).thenReturn(administrator);
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LegalDocument result = legalDocumentService.createNewVersion(validDocument, principal);

        assertThat(result.getUser()).isSameAs(administrator);
    }

    // ---------------------------------------------------------------
    // findLatestByType
    // ---------------------------------------------------------------

    @Test
    void findLatestByType_noDocumentOfThatType_throwsNotFound() {
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.GDPR_POLICY))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> legalDocumentService.findLatestByType(DocumentType.GDPR_POLICY));
    }

    @Test
    void findLatestByType_existingDocument_returnsIt() throws HttpException {
        LegalDocument document = new LegalDocument();
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.GDPR_POLICY))
                .thenReturn(Optional.of(document));

        assertThat(legalDocumentService.findLatestByType(DocumentType.GDPR_POLICY)).isEqualTo(document);
    }

    // ---------------------------------------------------------------
    // createNewVersion : nettoyage du contenu
    // ---------------------------------------------------------------

    private LegalDocument createWithContent(String content) throws HttpException {
        validDocument.setContent(content);
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.TERM_OF_USE))
                .thenReturn(Optional.empty());
        when(appUserService.findById(1L)).thenReturn(administrator);
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return legalDocumentService.createNewVersion(validDocument, principal);
    }

    @Test
    void createNewVersion_nonBreakingSpaces_areReplacedByNormalSpaces() throws HttpException {
        LegalDocument result = createWithContent("<p>Conditions&nbsp;generales\u00a0d'utilisation</p>");

        assertThat(result.getContent()).doesNotContain("&nbsp;").doesNotContain("\u00a0");
        assertThat(result.getContent()).contains("Conditions generales d'utilisation");
    }

    @Test
    void createNewVersion_scriptTag_isStrippedFromContent() throws HttpException {
        LegalDocument result = createWithContent("<p>Texte</p><script>alert('xss')</script>");

        assertThat(result.getContent()).doesNotContain("script").doesNotContain("alert");
        assertThat(result.getContent()).contains("Texte");
    }

    @Test
    void createNewVersion_eventHandlerAttribute_isStrippedFromContent() throws HttpException {
        LegalDocument result = createWithContent("<p onclick=\"alert(1)\">Texte</p>");

        assertThat(result.getContent()).doesNotContain("onclick");
    }

    @Test
    void createNewVersion_formattingTags_areKept() throws HttpException {
        LegalDocument result = createWithContent("<h2>Titre</h2><p><strong>Gras</strong></p><ul><li>Un</li></ul>");

        assertThat(result.getContent()).contains("<h2>", "<strong>", "<ul>", "<li>");
    }

    @Test
    void createNewVersion_classAttribute_isKept() throws HttpException {
        LegalDocument result = createWithContent("<p class=\"ql-align-center\">Centre</p>");

        assertThat(result.getContent()).contains("ql-align-center");
    }

    @Test
    void createNewVersion_contentEmptyAfterSanitizing_throwsBadRequest() {
        validDocument.setContent("<script>alert(1)</script>");

        assertThrows(BadRequestException.class, () -> legalDocumentService.createNewVersion(validDocument, principal));
    }
}
