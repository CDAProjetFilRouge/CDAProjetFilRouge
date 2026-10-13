package fr.diginamic.hubevenementiel.services;


import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

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
    public byte[] generateEventPDF(Long eventId) throws IOException, NotFoundException {
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
    public byte[] generateEventPDF(Long eventId, Long userId) throws IOException, NotFoundException {

        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("No event found with id"+eventId));

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
                + "@page { size: A4; margin: 0; }"
                + "body { font-family: sans-serif; color: #1f2430; font-size: 12px; margin: 0; }"
                + ".header { background-color: #e2536b; color: #ffffff; padding: 18px 40px; font-size: 16px; font-weight: bold; }"
                + ".content { padding: 32px 40px 0 40px; }"
                + ".badge { display: inline-block; background-color: #fbd3d9; color: #cf445c; font-size: 10px; font-weight: bold; padding: 4px 10px; }"
                + "h1 { font-size: 26px; margin: 14px 0 4px 0; color: #1f2430; }"
                + ".club { color: #6b7280; font-size: 13px; margin: 0 0 24px 0; }"
                + ".card { background-color: #f4f6f9; border-left: 4px solid #e2536b; padding: 12px 16px; margin-bottom: 12px; }"
                + ".label { color: #6b7280; font-size: 9px; font-weight: bold; margin-bottom: 4px; }"
                + ".value { font-size: 13px; line-height: 1.5; }"
                + "table.grid { width: 100%; border-collapse: separate; border-spacing: 0; }"
                + "table.grid td { width: 50%; vertical-align: top; padding: 0; }"
                + "h2 { font-size: 15px; margin: 24px 0 8px 0; padding-bottom: 6px; border-bottom: 1px solid #d8dce3; }"
                + ".description { font-size: 12px; line-height: 1.6; }"
                + ".footer { position: fixed; bottom: 0; left: 0; right: 0; padding: 12px 40px; background-color: #fcf8f5; color: #6b7280; font-size: 9px; }"
                + "</style></head><body>"
                + "<div class=\"header\">MyHubEvent</div>"
                + "<div class=\"content\">"
                + "<span class=\"badge\">" + escape(category) + "</span>"
                + "<h1>" + escape(event.getTitle()) + "</h1>"
                + (club.isEmpty() ? "" : "<p class=\"club\">Organisé par " + club + "</p>")
                + "<div class=\"card\"><div class=\"label\">DATE ET HORAIRES</div><div class=\"value\">" + when + "</div></div>"
                + (address.length() == 0 ? "" : "<div class=\"card\"><div class=\"label\">LIEU</div><div class=\"value\">" + address + "</div></div>")
                + priceCards(event, userId)
                + "<div class=\"card\"><div class=\"label\">CAPACITÉ</div><div class=\"value\">" + event.getMaxCapacity() + " places</div></div>"
                + "<h2>À propos</h2>"
                + "<div class=\"description\">" + description + "</div>"
                + "</div>"
                + "<div class=\"footer\">MyHubEvent – Fiche événement éditée le "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "</div>"
                + "</body></html>";

        return renderHtml(html);
    }

    /**
     * @param eventId id of the event
     * @return a file name (without extension) derived from the event title, safe for use in a download header
     * @throws NotFoundException if the event does not exist
     */
    public String eventPdfFileName(Long eventId) throws NotFoundException {
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("No event found with id"+eventId));
        String name = event.getTitle().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return name.isEmpty() ? "Event-" + eventId : name;
    }

    private String priceCards(Event event, Long userId) {
        AppUser user = userId == null ? null : userRepo.findById(userId).orElse(null);
        if (user != null) {
            boolean affiliated = user.getClubs().stream().anyMatch(
                    club -> club.getEndValidityDate() == null || club.getEndValidityDate().isAfter(LocalDate.now()));
            BigDecimal price = affiliated ? event.getAffiliatePrice() : event.getNonAffiliatePrice();
            return "<div class=\"card\"><div class=\"label\">VOTRE TARIF ("
                    + (affiliated ? "ADHÉRENT" : "NON ADHÉRENT") + ")</div><div class=\"value\">"
                    + formatPrice(price) + "</div></div>";
        }
        return "<table class=\"grid\"><tr>"
                + "<td><div class=\"card\" style=\"margin-right: 6px;\"><div class=\"label\">TARIF ADHÉRENT</div><div class=\"value\">" + formatPrice(event.getAffiliatePrice()) + "</div></div></td>"
                + "<td><div class=\"card\" style=\"margin-left: 6px;\"><div class=\"label\">TARIF NON ADHÉRENT</div><div class=\"value\">" + formatPrice(event.getNonAffiliatePrice()) + "</div></div></td>"
                + "</tr></table>";
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
    public String legalDocumentPdfFileName(Long id) throws NotFoundException {
        LegalDocument legalDocument = legalDocumentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aucun document avec l'id: " + id));
        return legalDocumentTitle(legalDocument).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String legalDocumentTitle(LegalDocument legalDocument) {
        return legalDocument.getDocumentType() == DocumentType.TERM_OF_USE
                ? "Conditions d'utilisation"
                : "Politique de confidentialité (RGPD)";
    }

    public byte[] generateCUPDF(Long id) throws IOException, NotFoundException {
        LegalDocument legalDocument = legalDocumentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aucun document avec l'id: " + id));

        String title = legalDocumentTitle(legalDocument);

        String date = legalDocument.getUpdateDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String html = "<html><head><style>"
                + "body { font-family: sans-serif; font-size: 12px; }"
                + ".footer { margin-top: 30px; color: #666; font-size: 10px; }"
                + "</style></head><body>"
                + "<h1>" + title + "</h1>"
                + legalDocument.getContent()
                + "<p class=\"footer\">Version " + legalDocument.getVersion() + " | Dernière mise à jour : " + date + "</p>"
                + "</body></html>";

        return renderHtml(html);
    }
}
