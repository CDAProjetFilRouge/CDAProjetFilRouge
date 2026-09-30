package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
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

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    private LegalDocument validDocument;

    @BeforeEach
    void setUp() {
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
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LegalDocument result = legalDocumentService.createNewVersion(validDocument);

        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void createNewVersion_existingVersions_incrementsFromLatest() throws HttpException {
        LegalDocument previous = new LegalDocument();
        previous.setVersion(3);
        when(legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(DocumentType.TERM_OF_USE))
                .thenReturn(Optional.of(previous));
        when(legalDocumentRepo.save(any(LegalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LegalDocument result = legalDocumentService.createNewVersion(validDocument);

        assertThat(result.getVersion()).isEqualTo(4);
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
}
