package fr.diginamic.hubevenementiel.services;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PDFService {

    private final EventRepo eventRepo;
    private final LegalDocumentRepo legalDocumentRepo;

    public PDFService(EventRepo eventRepo, LegalDocumentRepo legalDocumentRepo){
        this.eventRepo = eventRepo;
        this.legalDocumentRepo = legalDocumentRepo;
    }

    /**
     *
     * @param eventId id of the event we want to download a PDF of
     * @return An array of byte containing out PDF data
     * @throws IOException
     * @throws NotFoundException
     */
    public byte[] generateEventPDF(Long eventId) throws IOException, NotFoundException {

        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("No event found with id"+eventId));

        StringBuilder address = new StringBuilder();
        address.append(event.getLocation().getStreet1() + ", ");
        if(event.getLocation().getStreet2() != null){
            address.append(event.getLocation().getStreet2()+ ", ");
        }
        address.append(event.getLocation().getCity()+ ", ");
        address.append(event.getLocation().getPostalCode());

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(output);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(event.getTitle()));
        document.add(new Paragraph(event.getCategory().name()));
        document.add(new Paragraph("Du :"+event.getStartDateTime()+" au :"+event.getEndDateTime()));
        document.add(new Paragraph(event.getDescription()));
        document.add(new Paragraph("Prix adhérent: "+event.getAffiliatePrice()+" € | Prix non adhérent: "+event.getNonAffiliatePrice()+" €"));
        document.add(new Paragraph("Nombre de place disponible: "+event.getMaxCapacity()));
        document.add(new Paragraph(address.toString()));

        document.close();

        return output.toByteArray();

    }


    public byte[] generateCUPDF(Long id) throws IOException, NotFoundException {
        LegalDocument legalDocument = legalDocumentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Aucun document avec l'id: " + id));

        String title = legalDocument.getDocumentType() == DocumentType.TERM_OF_USE
                ? "Conditions d'utilisation"
                : "Politique de confidentialité (RGPD)";

        String date = legalDocument.getUpdateDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String html = "<html><head><style>"
                + "body { font-family: sans-serif; font-size: 12px; }"
                + ".footer { margin-top: 30px; color: #666; font-size: 10px; }"
                + "</style></head><body>"
                + "<h1>" + title + "</h1>"
                + legalDocument.getContent()
                + "<p class=\"footer\">Version " + legalDocument.getVersion() + " | Dernière mise à jour : " + date + "</p>"
                + "</body></html>";

        org.jsoup.nodes.Document parsed = Jsoup.parse(html);
        parsed.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        String xhtml = parsed.html();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(xhtml, null);
        builder.toStream(output);
        builder.run();
        return output.toByteArray();
    }
}
