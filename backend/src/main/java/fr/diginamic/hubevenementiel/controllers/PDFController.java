package fr.diginamic.hubevenementiel.controllers;

import java.util.UUID;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.openapi.PdfApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.PDFService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/PDF")
public class PDFController implements PdfApi {

    private final PDFService pdfService;

    public PDFController(PDFService pdfService){
        this.pdfService = pdfService;
    }

    @Override
    @GetMapping("/event/{idEvent}")
    public ResponseEntity<byte[]> eventPDF(@PathVariable UUID idEvent) throws IOException, NotFoundException {
        // The endpoint is public: the principal is only present when a valid token was sent.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal
                ? principal.id()
                : null;

        byte[] pdf = pdfService.generateEventPDF(idEvent, userId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdfService.eventPdfFileName(idEvent) + ".pdf", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(pdf);
    }

    @Override
    @GetMapping("/legalDocument/{idDocument}")
    public ResponseEntity<byte[]> legalDocumentPDF(@PathVariable UUID idDocument) throws IOException, NotFoundException {
        byte[] pdf = pdfService.generateCUPDF(idDocument);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(pdfService.legalDocumentPdfFileName(idDocument) + ".pdf", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(pdf);
    }
}
