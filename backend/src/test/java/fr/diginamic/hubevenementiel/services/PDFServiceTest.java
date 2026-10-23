package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private PDFService pdfService;

    private LegalDocument document;

    @BeforeEach
    void setUp() {
        document = new LegalDocument();
        document.setId(uuid(1));
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
        when(legalDocumentRepo.findById(uuid(99))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> pdfService.generateCUPDF(uuid(99)));
    }

    @Test
    void generateCUPDF_validDocument_returnsAPdf() throws IOException, NotFoundException {
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(uuid(1)))).isTrue();
    }

    @Test
    void generateCUPDF_unclosedBrTag_stillProducesAPdf() throws IOException, NotFoundException {
        // Quill produit du HTML : une balise <br> non fermée ferait échouer un parseur XML strict.
        document.setContent("<p>Ligne un<br>Ligne deux</p><p>Suite</p>");
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(uuid(1)))).isTrue();
    }

    @Test
    void generateCUPDF_gdprDocument_returnsAPdf() throws IOException, NotFoundException {
        document.setDocumentType(DocumentType.GDPR_POLICY);
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(uuid(1)))).isTrue();
    }

    @Test
    void generateCUPDF_listsAndSpecialCharacters_stillProducesAPdf() throws IOException, NotFoundException {
        document.setContent("<ul><li>Données &amp; vie privée</li><li>Café &lt;test&gt;</li></ul><p>À bientôt</p>");
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        assertThat(isPdf(pdfService.generateCUPDF(uuid(1)))).isTrue();
    }

    // ---------------------------------------------------------------
    // Mise en page : texte réellement écrit dans les PDF
    // ---------------------------------------------------------------

    private static String text(byte[] pdf) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(document).replace('\u00a0', ' ');
        }
    }

    // Les étiquettes en capitales espacées sont extraites lettre par lettre : on les compare sans espaces.
    private static String squash(String text) {
        return text.replaceAll("\s+", "");
    }

    private static int pages(byte[] pdf) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            return document.getNumberOfPages();
        }
    }

    private Event event(String title) {
        Address address = new Address();
        address.setStreet1("12 rue de la Paix");
        address.setPostalCode("35000");
        address.setCity("Rennes");
        Club club = new Club();
        club.setName("Club Athlétisme Rennais");
        Event event = new Event();
        event.setId(uuid(2));
        event.setTitle(title);
        event.setDescription("Une journée de matchs en double.\nPrévoir des chaussures de salle.");
        event.setCategory(Category.SPORT);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartDateTime(LocalDateTime.of(2030, 11, 16, 9, 0));
        event.setEndDateTime(LocalDateTime.of(2030, 11, 16, 17, 30));
        event.setAffiliatePrice(new BigDecimal("6"));
        event.setNonAffiliatePrice(new BigDecimal("10"));
        event.setMaxCapacity(48);
        event.setLocation(address);
        event.setClub(club);
        return event;
    }

    @Test
    void generateEventPDF_anonymous_showsBothPricesTheEventAndAFooterWithPages() throws Exception {
        when(eventRepo.findById(uuid(2))).thenReturn(Optional.of(event("Tournoi de badminton")));

        byte[] pdf = pdfService.generateEventPDF(uuid(2));

        String text = text(pdf);
        assertThat(isPdf(pdf)).isTrue();
        assertThat(squash(text)).contains("FICHEÉVÈNEMENT").contains("DATEETHORAIRES").contains("LIEU")
                .contains("TARIFS").contains("CAPACITÉ");
        assertThat(text).contains("MyHubEvent").contains("SPORT")
                .contains("Tournoi de badminton").contains("Organisé par Club Athlétisme Rennais")
                .contains("samedi 16 novembre 2030").contains("09h00 – 17h30")
                .contains("12 rue de la Paix").contains("35000 Rennes")
                .contains("Adhérent").contains("6,00 €").contains("Non adhérent").contains("10,00 €")
                .contains("48 places")
                .contains("À propos").contains("Prévoir des chaussures de salle.")
                .contains("Fiche éditée le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .contains("Page 1 / 1");
    }

    @Test
    void generateEventPDF_knownUser_showsOnlyTheirOwnPrice() throws Exception {
        Event event = event("Tournoi de badminton");
        when(eventRepo.findById(uuid(2))).thenReturn(Optional.of(event));
        Club affiliation = new Club();
        affiliation.setName("Club A");
        AppUser member = new AppUser();
        member.setClubs(List.of(affiliation));
        when(userRepo.findById(uuid(6))).thenReturn(Optional.of(member));
        AppUser outsider = new AppUser();
        outsider.setClubs(List.of());
        when(userRepo.findById(uuid(7))).thenReturn(Optional.of(outsider));

        String affiliated = text(pdfService.generateEventPDF(uuid(2), uuid(6)));
        String notAffiliated = text(pdfService.generateEventPDF(uuid(2), uuid(7)));

        assertThat(squash(affiliated)).contains("VOTRETARIF(ADHÉRENT)");
        assertThat(affiliated).contains("6,00 €").doesNotContain("10,00 €");
        assertThat(squash(notAffiliated)).contains("VOTRETARIF(NONADHÉRENT)");
        assertThat(notAffiliated).contains("10,00 €").doesNotContain("6,00 €");
    }

    @Test
    void generateEventPDF_freeEvent_saysGratuit() throws Exception {
        Event event = event("Portes ouvertes");
        event.setAffiliatePrice(BigDecimal.ZERO);
        event.setNonAffiliatePrice(BigDecimal.ZERO);
        when(eventRepo.findById(uuid(2))).thenReturn(Optional.of(event));

        assertThat(text(pdfService.generateEventPDF(uuid(2)))).contains("Gratuit").doesNotContain("0,00");
    }

    @Test
    void generateEventPDF_htmlInTitleAndDescription_isWrittenAsTextNotInterpreted() throws Exception {
        Event event = event("<b>Tournoi</b> & Cie");
        event.setDescription("<script>alert(1)</script> <img src=\"http://exemple.invalid/x.png\"> Texte");
        when(eventRepo.findById(uuid(2))).thenReturn(Optional.of(event));

        String text = text(pdfService.generateEventPDF(uuid(2)));

        assertThat(text).contains("<b>Tournoi</b> & Cie").contains("<script>alert(1)</script>");
    }

    @Test
    void generateEventPDF_longDescription_flowsOnSeveralPagesWithTheFooterOnEach() throws Exception {
        Event event = event("Tournoi de badminton");
        event.setDescription(("Une longue description qui occupe de la place sur la page. ").repeat(300));
        when(eventRepo.findById(uuid(2))).thenReturn(Optional.of(event));

        byte[] pdf = pdfService.generateEventPDF(uuid(2));

        assertThat(pages(pdf)).isGreaterThan(1);
        assertThat(text(pdf)).contains("Page 1 / " + pages(pdf)).contains("Page " + pages(pdf) + " / " + pages(pdf));
    }

    @Test
    void generateCUPDF_hasTheHeaderTheVersionAndTheDocumentText() throws Exception {
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        String text = text(pdfService.generateCUPDF(uuid(1)));

        assertThat(squash(text)).contains("DOCUMENTLÉGAL");
        assertThat(text).contains("MyHubEvent").contains("Conditions d'utilisation")
                .contains("Version 2").contains("Dernière mise à jour le 09/10/2026")
                .contains("Article 1").contains("Texte important")
                .contains("Conditions d'utilisation · Version 2").contains("Page 1 / 1");
    }

    @Test
    void generateCUPDF_longDocument_flowsOnSeveralPagesWithPageNumbers() throws Exception {
        document.setContent(("<h2>Article</h2><p>Un paragraphe de texte juridique assez long pour remplir la page. </p>").repeat(80));
        when(legalDocumentRepo.findById(uuid(1))).thenReturn(Optional.of(document));

        byte[] pdf = pdfService.generateCUPDF(uuid(1));

        assertThat(pages(pdf)).isGreaterThan(1);
        assertThat(text(pdf)).contains("Page 2 / " + pages(pdf));
    }
}
