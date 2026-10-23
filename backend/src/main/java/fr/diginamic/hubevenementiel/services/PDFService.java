package fr.diginamic.hubevenementiel.services;


import java.util.UUID;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PDFService {

    private final EventRepo eventRepo;
    private final LegalDocumentRepo legalDocumentRepo;
    private final UserRepo userRepo;

    public PDFService(EventRepo eventRepo, LegalDocumentRepo legalDocumentRepo, UserRepo userRepo){
        this.eventRepo = eventRepo;
        this.legalDocumentRepo = legalDocumentRepo;
        this.userRepo = userRepo;
    }

    /**
     *
     * @param eventId id of the event we want to download a PDF of
     * @return An array of byte containing out PDF data
     * @throws IOException
     * @throws NotFoundException
     */
    public byte[] generateEventPDF(UUID eventId) throws IOException, NotFoundException {
        return generateEventPDF(eventId, null);
    }

    /**
     * @param eventId id of the event we want to download a PDF of
     * @param userId  id of the requesting user, or null if anonymous. When known, only the price that applies to
     *                this user (affiliate or not) is displayed.
     * @return An array of byte containing out PDF data
     * @throws IOException
     * @throws NotFoundException
     */
    @Transactional(readOnly = true)
    public byte[] generateEventPDF(UUID eventId, UUID userId) throws IOException, NotFoundException {

        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("No event found with id"+eventId));

        if (event.getStatus() == EventStatus.DRAFT) {
            AppUser caller = userId == null ? null : userRepo.findById(userId).orElse(null);
            boolean isAdmin = caller != null && caller.getRole() == Role.ADMINISTRATOR;
            boolean isOrganizer = caller != null && event.getOrganizer() != null
                    && event.getOrganizer().getId().equals(caller.getId());
            if (!isAdmin && !isOrganizer) {
                throw new NotFoundException("No event found with id" + eventId);
            }
        }

        Address location = event.getLocation();
        StringBuilder address = new StringBuilder();
        if (location != null) {
            address.append(escape(location.getStreet1()));
            if (location.getStreet2() != null && !location.getStreet2().isBlank()) {
                address.append("<br/>").append(escape(location.getStreet2()));
            }
            address.append("<br/>").append(escape(location.getPostalCode())).append(" ").append(escape(location.getCity()));
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH'h'mm");
        LocalDateTime start = event.getStartDateTime();
        LocalDateTime end = event.getEndDateTime();
        String when = start.toLocalDate().equals(end.toLocalDate())
                ? start.format(dateFormat) + "<br/>" + start.format(timeFormat) + " – " + end.format(timeFormat)
                : "Du " + start.format(dateFormat) + " à " + start.format(timeFormat)
                    + "<br/>au " + end.format(dateFormat) + " à " + end.format(timeFormat);

        String category = event.getCategory().name();
        String club = event.getClub() != null ? escape(event.getClub().getName()) : "";
        String description = escape(event.getDescription()).replace("\n", "<br/>");

        String html = "<html><head><meta charset=\"UTF-8\"/><title>" + escape(event.getTitle()) + "</title><style>"
                + pdfCss("MyHubEvent · Fiche éditée le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                + "</style></head><body>"
                + pdfHeader("FICHE ÉVÈNEMENT")
                + "<span class=\"cat\">" + escape(category) + "</span>"
                + "<h1>" + escape(event.getTitle()) + "</h1>"
                + (club.isEmpty() ? "" : "<p class=\"club\">Organisé par " + club + "</p>")
                + "<table class=\"grid\"><tr>"
                + "<td class=\"cell left\"><div class=\"box\"><div class=\"label\">DATE ET HORAIRES</div><div class=\"value\">" + when + "</div></div></td>"
                + "<td class=\"cell right\">" + (address.length() == 0 ? "" : "<div class=\"box\"><div class=\"label\">LIEU</div><div class=\"value\">" + address + "</div></div>") + "</td>"
                + "</tr></table>"
                + "<table class=\"grid\"><tr>"
                + priceBox(event, userId)
                + "<td class=\"cell right\"><div class=\"box\"><div class=\"label\">CAPACITÉ</div><div class=\"value\">" + event.getMaxCapacity() + " places</div></div></td>"
                + "</tr></table>"
                + "<h2>À propos</h2>"
                + "<div class=\"description\">" + description + "</div>"
                + "</body></html>";

        return renderHtml(html);
    }

    /**
     * @param eventId id of the event
     * @return a file name (without extension) derived from the event title, safe for use in a download header
     * @throws NotFoundException if the event does not exist
     */
    public String eventPdfFileName(UUID eventId) throws NotFoundException {
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("No event found with id"+eventId));
        String name = event.getTitle().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return name.isEmpty() ? "Event-" + eventId : name;
    }

    private String priceBox(Event event, UUID userId) {
        AppUser user = userId == null ? null : userRepo.findById(userId).orElse(null);
        if (user != null) {
            boolean affiliated = user.getClubs().stream().anyMatch(
                    club -> club.getEndValidityDate() == null || club.getEndValidityDate().isAfter(LocalDate.now()));
            BigDecimal price = affiliated ? event.getAffiliatePrice() : event.getNonAffiliatePrice();
            return "<td class=\"cell left\"><div class=\"box accent\"><div class=\"label\">VOTRE TARIF ("
                    + (affiliated ? "ADHÉRENT" : "NON ADHÉRENT") + ")</div><div class=\"value price\">"
                    + formatPrice(price) + "</div></div></td>";
        }
        return "<td class=\"cell left\"><div class=\"box accent\"><div class=\"label\">TARIFS</div>"
                + "<table class=\"prices\"><tr><td>Adhérent</td><td class=\"amount\">" + formatPrice(event.getAffiliatePrice()) + "</td></tr>"
                + "<tr><td>Non adhérent</td><td class=\"amount\">" + formatPrice(event.getNonAffiliatePrice()) + "</td></tr></table>"
                + "</div></td>";
    }

    /** Styles communs aux PDF : page A4, pied de page (texte et pagination), et les composants de l'identité visuelle. */
    private static String pdfCss(String footerLeft) {
        return "@page { size: A4; margin: 16mm 15mm 20mm 15mm;"
                + " @bottom-left { content: \"" + cssString(footerLeft) + "\"; font-family: sans-serif; font-size: 8.5px; color: #5b5d73; }"
                + " @bottom-right { content: \"Page \" counter(page) \" / \" counter(pages); font-family: sans-serif; font-size: 8.5px; color: #5b5d73; } }"
                + "body { font-family: sans-serif; color: #17182b; font-size: 11.5px; line-height: 1.55; margin: 0; }"
                + "table.head { width: 100%; background-color: #17182b; border-radius: 10px; }"
                + "table.head td { padding: 13px 20px; vertical-align: middle; }"
                + "td.brand { color: #ffffff; font-size: 19px; font-weight: bold; }"
                + "td.brand img { vertical-align: middle; }"
                + "span.word { padding-left: 8px; }"
                + "span.ev { color: #8fa2ff; }"
                + "td.kind { text-align: right; color: #ffd8a6; font-size: 9px; font-weight: bold; letter-spacing: 2px; }"
                + "div.perf { border-bottom: 2px dashed #e2622b; margin: 8px 0 22px 0; height: 0; }"
                + "span.cat { background-color: #ffd8a6; color: #7a3512; font-size: 9px; font-weight: bold; letter-spacing: 1px; padding: 4px 11px; border-radius: 10px; }"
                + "h1 { font-size: 26px; line-height: 1.15; margin: 14px 0 4px 0; color: #17182b; }"
                + "p.club { color: #5b5d73; font-size: 12.5px; margin: 0 0 22px 0; }"
                + "table.grid { width: 100%; border-collapse: collapse; margin-bottom: 10px; }"
                + "td.cell { width: 50%; vertical-align: top; padding: 0; }"
                + "td.left { padding-right: 5px; }"
                + "td.right { padding-left: 5px; }"
                + "div.box { border: 1px solid #e3dcc8; border-radius: 9px; padding: 11px 15px; }"
                + "div.accent { background-color: #dfe5ff; border-color: #dfe5ff; }"
                + "div.label { color: #5b5d73; font-size: 8px; font-weight: bold; letter-spacing: 1.5px; margin-bottom: 4px; }"
                + "div.accent div.label { color: #1f37a8; }"
                + "div.value { font-size: 12.5px; line-height: 1.5; }"
                + "div.price { font-size: 17px; font-weight: bold; color: #1f37a8; }"
                + "table.prices { width: 100%; border-collapse: collapse; }"
                + "table.prices td { padding: 1px 0; font-size: 12px; }"
                + "table.prices td.amount { text-align: right; font-weight: bold; }"
                + "h2 { font-size: 14px; margin: 22px 0 8px 0; padding-bottom: 5px; border-bottom: 2px solid #2a46c9; }"
                + "div.description { font-size: 11.5px; line-height: 1.65; }"
                + "h1.doc { font-size: 24px; margin: 4px 0 6px 0; }"
                + "p.version { color: #5b5d73; font-size: 10.5px; margin: 0 0 20px 0; }"
                + "span.versionbadge { background-color: #d6ebe2; color: #14513c; font-weight: bold; padding: 2px 9px; border-radius: 9px; font-size: 9.5px; }"
                + "div.legal h2 { font-size: 15px; border-bottom: 0; margin: 20px 0 6px 0; padding-bottom: 0; color: #17182b; }"
                + "div.legal h3 { font-size: 13px; margin: 16px 0 5px 0; }"
                + "div.legal p { margin: 0 0 9px 0; font-size: 11px; line-height: 1.65; }"
                + "div.legal li { margin-bottom: 4px; font-size: 11px; line-height: 1.6; }"
                + "div.legal a { color: #2a46c9; }";
    }

    /** En-tête commun : logo et nom sur fond sombre, type de document à droite, puis la perforation du billet. */
    private String pdfHeader(String kind) {
        return "<table class=\"head\"><tr>"
                + "<td class=\"brand\"><img src=\"" + logoDataUri() + "\" width=\"34\" height=\"28\"/>"
                + "<span class=\"word\">MyHub<span class=\"ev\">Event</span></span></td>"
                + "<td class=\"kind\">" + kind + "</td></tr></table>"
                + "<div class=\"perf\"></div>";
    }

    private String logoDataUri() {
        try (java.io.InputStream in = new org.springframework.core.io.ClassPathResource("pdf/logo.png").getInputStream()) {
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Logo des PDF introuvable.", e);
        }
    }

    private static String cssString(String value) {
        return value.replace("\\", " ").replace("\"", "'").replaceAll("[\\r\\n]+", " ");
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    private static String formatPrice(BigDecimal price) {
        if (price == null || price.signum() == 0) {
            return "Gratuit";
        }
        return price.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',') + " €";
    }

    private byte[] renderHtml(String html) throws IOException {
        org.jsoup.nodes.Document parsed = Jsoup.parse(html);
        parsed.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(parsed.html(), null);
        builder.toStream(output);
        builder.run();
        return output.toByteArray();
    }


    /**
     * @param id id of the legal document
     * @return a file name (without extension) for the legal document PDF download
     * @throws NotFoundException if the document does not exist
     */
    public String legalDocumentPdfFileName(UUID id) throws NotFoundException {
        LegalDocument legalDocument = legalDocumentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aucun document avec l'id: " + id));
        return legalDocumentTitle(legalDocument).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String legalDocumentTitle(LegalDocument legalDocument) {
        return legalDocument.getDocumentType() == DocumentType.TERM_OF_USE
                ? "Conditions d'utilisation"
                : "Politique de confidentialité (RGPD)";
    }

    public byte[] generateCUPDF(UUID id) throws IOException, NotFoundException {
        LegalDocument legalDocument = legalDocumentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aucun document avec l'id: " + id));

        String title = legalDocumentTitle(legalDocument);

        String date = legalDocument.getUpdateDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String html = "<html><head><meta charset=\"UTF-8\"/><title>" + title + "</title><style>"
                + pdfCss(title + " · Version " + legalDocument.getVersion())
                + "</style></head><body>"
                + pdfHeader("DOCUMENT LÉGAL")
                + "<h1 class=\"doc\">" + title + "</h1>"
                + "<p class=\"version\"><span class=\"versionbadge\">Version " + legalDocument.getVersion() + "</span>"
                + "  Dernière mise à jour le " + date + "</p>"
                + "<div class=\"legal\">" + legalDocument.getContent() + "</div>"
                + "</body></html>";

        return renderHtml(html);
    }
}
