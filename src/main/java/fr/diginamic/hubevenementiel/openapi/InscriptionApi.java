package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionEventResponseDto;
import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionResponseDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Inscriptions", description = "Inscription des utilisateurs aux évènements, avec gestion de liste d'attente.")
@SecurityRequirement(name = "bearerAuth")
public interface InscriptionApi {

    @Operation(summary = "S'inscrire à un évènement",
            description = "Inscrit l'utilisateur à l'évènement s'il reste des places, sinon le place en liste d'attente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription créée (confirmée ou en liste d'attente)"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou évènement introuvable"),
            @ApiResponse(responseCode = "409", description = "Utilisateur déjà inscrit à cet évènement")
    })
    ResponseEntity<InscriptionResponseDto> register(
            @Parameter(description = "Identifiant de l'utilisateur", required = true) Long userId,
            @Parameter(description = "Identifiant de l'évènement", required = true) Long eventId) throws HttpException;

    @Operation(summary = "Se désinscrire d'un évènement",
            description = "Annulation par le membre lui-même. Libère une place pour le premier de la liste d'attente le cas échéant.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscription annulée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Inscription introuvable")
    })
    ResponseEntity<Void> cancelByMember(@Parameter(description = "Identifiant de l'inscription", required = true) Long id) throws HttpException;

    @Operation(summary = "Annuler l'inscription d'un membre en tant qu'organisateur",
            description = "Réservé à l'organisateur de l'évènement et aux administrateurs. Un email est envoyé au membre avec le motif fourni.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscription annulée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER/ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Inscription introuvable")
    })
    ResponseEntity<Void> cancelByOrganizer(@Parameter(description = "Identifiant de l'inscription", required = true) Long id,
            @Parameter(description = "Motif de l'annulation, communiqué au membre par email", required = true) String motif) throws HttpException;

    @Operation(summary = "Lister les inscriptions d'un évènement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des inscriptions"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Évènement introuvable")
    })
    List<InscriptionEventResponseDto> getByEvent(@Parameter(description = "Identifiant de l'évènement", required = true) Long eventId,
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size) throws HttpException;

    @Operation(summary = "Lister les inscriptions d'un utilisateur", description = "Sert notamment à construire le calendrier d'évènements de l'utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des inscriptions"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    List<InscriptionResponseDto> getByUser(@Parameter(description = "Identifiant de l'utilisateur", required = true) Long userId,
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size) throws HttpException;
}
