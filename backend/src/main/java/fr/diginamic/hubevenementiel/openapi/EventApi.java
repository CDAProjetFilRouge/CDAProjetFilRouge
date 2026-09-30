package fr.diginamic.hubevenementiel.openapi;

import java.time.LocalDateTime;
import java.util.List;

import fr.diginamic.hubevenementiel.dtos.event.EventRequestDto;
import fr.diginamic.hubevenementiel.dtos.event.EventResponseDto;
import fr.diginamic.hubevenementiel.dtos.event.EventSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Évènements", description = "Cycle de vie des évènements (brouillon, publié, annulé, terminé) et recherche multicritères.")
@SecurityRequirement(name = "bearerAuth")
public interface EventApi {

    @Operation(summary = "Rechercher des évènements",
            description = "Recherche paginée et multicritères (catégorie, période, prix, statut). "
                    + "Les évènements au statut DRAFT ne sont visibles que par leur organisateur et les administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des évènements"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    List<EventSummaryResponseDto> getEvents(
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size,
            @Parameter(description = "Filtre optionnel par catégorie") Category category,
            @Parameter(description = "Date/heure de début minimale") LocalDateTime startDate,
            @Parameter(description = "Date/heure de fin maximale") LocalDateTime endDate,
            @Parameter(description = "Prix minimum") Integer minPrice,
            @Parameter(description = "Prix maximum") Integer maxPrice,
            @Parameter(description = "Filtre optionnel par statut") EventStatus status) throws HttpException;

    @Operation(summary = "Consulter le détail d'un évènement",
            description = "Un évènement au statut DRAFT n'est visible que par son organisateur et les administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Évènement trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable ou non visible pour l'appelant")
    })
    EventResponseDto getById(@Parameter(description = "Identifiant de l'évènement", required = true) Long id) throws HttpException;

    @Operation(summary = "Rechercher un évènement par titre exact")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Évènement trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Aucun évènement avec ce titre")
    })
    EventResponseDto getByName(@Parameter(description = "Titre de l'évènement", required = true) String name) throws HttpException;

    @Operation(summary = "Créer un évènement",
            description = "Réservé aux organisateurs et aux administrateurs. L'évènement est créé au statut BROUILLON avec l'appelant comme organisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Évènement créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis")
    })
    ResponseEntity<EventResponseDto> create(EventRequestDto requestDto) throws HttpException;

    @Operation(summary = "Modifier un évènement",
            description = "Réservé au propriétaire de l'évènement et aux administrateurs. Pour un évènement passé, seul l'ajout d'images à la galerie est autorisé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Évènement mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis ou évènement dont l'appelant n'est pas propriétaire"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    EventResponseDto update(@Parameter(description = "Identifiant de l'évènement", required = true) Long id, EventRequestDto requestDto) throws HttpException;

    @Operation(summary = "Supprimer un évènement",
            description = "Réservé au propriétaire d'un évènement futur et aux administrateurs. Un email est envoyé aux inscrits et à la liste d'attente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Évènement supprimé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis ou évènement dont l'appelant n'est pas propriétaire"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    ResponseEntity<Void> delete(@Parameter(description = "Identifiant de l'évènement", required = true) Long id) throws HttpException;
}
