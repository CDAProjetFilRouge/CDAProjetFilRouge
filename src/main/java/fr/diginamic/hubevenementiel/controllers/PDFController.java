package fr.diginamic.hubevenementiel.controllers;

import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.services.PDFService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/PDF")
public class PDFController {

    private final PDFService pdfService;

    public PDFController(PDFService pdfService){
        this.pdfService = pdfService;
    }

    /**
     *
     * @param idEvent id of the event we want to download a PDF of
     * @return a responseEntity containing the file
     * @throws IOException
     * @throws NotFoundException
     */
    @GetMapping("/event/{idEvent}")
    public ResponseEntity<byte[]> eventPDF(@PathVariable Long idEvent) throws IOException, NotFoundException {
        byte[] pdf = pdfService.generateEventPDF(idEvent);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"Event-"+idEvent+".pdf\"").body(pdf);
    }

    /**
     *
     * @param idDocument id of the document we want to download a PDF of
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    @GetMapping("/legalDocument/{idDocument}")
    public ResponseEntity<byte[]> legalDocumentPDF(@PathVariable Long idDocument) throws IOException, NotFoundException {
        byte[] pdf = pdfService.generateCUPDF(idDocument);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"LegalDocument-"+idDocument+"/pdf\"").body(pdf);
    }
}
