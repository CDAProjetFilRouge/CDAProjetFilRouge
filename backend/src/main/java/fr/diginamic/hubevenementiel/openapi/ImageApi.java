package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.image.ImageSummaryResponseDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Galerie d'images", description = "Galerie d'images associée à un évènement.")
@SecurityRequirement(name = "bearerAuth")
public interface ImageApi {

    @Operation(summary = "Lister les images d'un évènement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des images de la galerie"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    List<ImageSummaryResponseDto> getByEvent(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId) throws HttpException;

    @Operation(summary = "Ajouter une image à la galerie d'un évènement",
            description = "Réservé à l'organisateur de l'évènement et aux administrateurs. Requête multipart/form-data.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image ajoutée"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide ou absent"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis ou évènement dont l'appelant n'est pas propriétaire"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    ResponseEntity<ImageSummaryResponseDto> upload(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Fichier image à uploader", required = true) MultipartFile file) throws HttpException;

    @Operation(summary = "Supprimer une image de la galerie",
            description = "Réservé à l'organisateur de l'évènement et aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image supprimée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis ou évènement dont l'appelant n'est pas propriétaire"),
            @ApiResponse(responseCode = "404", description = "Image ou évènement introuvable")
    })
    ResponseEntity<Void> delete(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Identifiant de l'image", required = true) Long id) throws HttpException;
}
