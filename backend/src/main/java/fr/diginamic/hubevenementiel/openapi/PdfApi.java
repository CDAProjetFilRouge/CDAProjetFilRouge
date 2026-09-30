package fr.diginamic.hubevenementiel.openapi;

import java.io.IOException;

import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "Export PDF", description = "Génération de fiches PDF téléchargeables. Accessible sans authentification.")
public interface PdfApi {

    @Operation(summary = "Télécharger la fiche PDF d'un évènement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier PDF généré",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    ResponseEntity<byte[]> eventPDF(@Parameter(description = "Identifiant de l'évènement", required = true) Long idEvent) throws IOException, NotFoundException;

    @Operation(summary = "Télécharger la fiche PDF d'un document légal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier PDF généré",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = "Document introuvable")
    })
    ResponseEntity<byte[]> legalDocumentPDF(@Parameter(description = "Identifiant du document légal", required = true) Long idDocument) throws IOException, NotFoundException;
}
