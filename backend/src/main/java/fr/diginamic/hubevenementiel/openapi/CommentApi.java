package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.comment.CommentResponseDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Commentaires", description = "Commentaires laissés par les utilisateurs sur les évènements.")
@SecurityRequirement(name = "bearerAuth")
public interface CommentApi {

    @Operation(summary = "Lister les commentaires d'un évènement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des commentaires"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    List<CommentResponseDto> getByEvent(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size) throws HttpException;

    @Operation(summary = "Ajouter un commentaire à un évènement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commentaire créé"),
            @ApiResponse(responseCode = "400", description = "Contenu vide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Évènement ou auteur introuvable")
    })
    ResponseEntity<CommentResponseDto> create(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Contenu du commentaire", required = true) String content) throws HttpException;

    @Operation(summary = "Modifier un commentaire", description = "Réservé à l'auteur du commentaire.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire mis à jour"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Seul l'auteur peut modifier son commentaire"),
            @ApiResponse(responseCode = "404", description = "Commentaire introuvable pour cet évènement")
    })
    ResponseEntity<CommentResponseDto> update(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Identifiant du commentaire", required = true) Long commentId,
            @Parameter(description = "Nouveau contenu du commentaire", required = true) String newContent) throws HttpException;

    @Operation(summary = "Supprimer un commentaire",
            description = "Réservé à l'auteur du commentaire, à l'organisateur de l'évènement ou à un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Commentaire supprimé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Droits insuffisants pour supprimer ce commentaire"),
            @ApiResponse(responseCode = "404", description = "Commentaire introuvable pour cet évènement")
    })
    ResponseEntity<Void> delete(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Identifiant du commentaire", required = true) Long commentId) throws HttpException;
}
