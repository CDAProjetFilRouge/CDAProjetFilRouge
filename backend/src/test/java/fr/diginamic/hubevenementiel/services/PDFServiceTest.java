package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PDFServiceTest {

    @Mock
    private EventRepo eventRepo;
    @Mock
    private LegalDocumentRepo legalDocumentRepo;

    @InjectMocks
    private PDFService pdfService;

    private LegalDocument document;

    @BeforeEach
    void setUp() {
        document = new LegalDocument();
        document.setId(1L);
        document.setDocumentType(DocumentType.TERM_OF_USE);
        document.setVersion(2);
        document.setUpdateDate(LocalDateTime.of(2026, 10, 9, 12, 0));
        document.setContent("<h2>Article 1</h2><p>Texte <strong>important</strong></p>");
    }

    private boolean isPdf(byte[] bytes) {
        return bytes.length > 4 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    @Test
    void generateCUPDF_unknownDocument_throwsNotFound() {
        when(legalDocumentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> pdfService.generateCUPDF(99L));
    }

    @Test
    void generateCUPDF_validDocument_returnsAPdf() throws IOException, NotFoundException {
        when(legalDocumentRepo.findById(1L)).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(1L))).isTrue();
    }

    @Test
    void generateCUPDF_unclosedBrTag_stillProducesAPdf() throws IOException, NotFoundException {
        // Quill produit du HTML : une balise <br> non fermée ferait échouer un parseur XML strict.
        document.setContent("<p>Ligne un<br>Ligne deux</p><p>Suite</p>");
        when(legalDocumentRepo.findById(1L)).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(1L))).isTrue();
    }

    @Test
    void generateCUPDF_gdprDocument_returnsAPdf() throws IOException, NotFoundException {
        document.setDocumentType(DocumentType.GDPR_POLICY);
        when(legalDocumentRepo.findById(1L)).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(1L))).isTrue();
    }

    @Test
    void generateCUPDF_listsAndSpecialCharacters_stillProducesAPdf() throws IOException, NotFoundException {
        document.setContent("<ul><li>Données &amp; vie privée</li><li>Café &lt;test&gt;</li></ul><p>À bientôt</p>");
        when(legalDocumentRepo.findById(1L)).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(1L))).isTrue();
    }
}
