package fr.diginamic.hubevenementiel.services;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        if(!event.getLocation().getStreet2().isEmpty()){
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

    /**
     *
     * @param id of the legal document we want to download a PDF of
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    public byte[] generateCUPDF(Long id) throws IOException, NotFoundException {

        LegalDocument conditionUtilisation = legalDocumentRepo.findById(id).orElseThrow(() -> new NotFoundException("No document found with id: "+id));

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(output);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(conditionUtilisation.getDocumentType().name()));
        document.add(new Paragraph(conditionUtilisation.getContent()));
        document.add(new Paragraph("Version : " + conditionUtilisation.getVersion() + " | Dernière mise à jour: " + conditionUtilisation.getUpdateDate()));

        document.close();

        return output.toByteArray();
    }
}
