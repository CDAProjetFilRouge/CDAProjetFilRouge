package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.anonymizerDemand.AnonymizationDemandResponseDto;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Demandes d'anonymisation", description = "Cycle de vie des demandes RGPD d'anonymisation des données personnelles.")
@SecurityRequirement(name = "bearerAuth")
public interface AnonymizationDemandApi {

    @Operation(summary = "Lister les demandes d'anonymisation", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des demandes"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis")
    })
    List<AnonymizationDemandResponseDto> getDemands(
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size,
            @Parameter(description = "Filtre optionnel par statut de la demande") RequestStatus status) throws HttpException;

    @Operation(summary = "Demander l'anonymisation de ses propres données",
            description = "L'utilisateur connecté ne pourra plus utiliser la plateforme une fois la demande validée par un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande enregistrée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "409", description = "Une demande est déjà en cours pour cet utilisateur")
    })
    ResponseEntity<AnonymizationDemandResponseDto> request() throws HttpException;

    @Operation(summary = "Valider une demande d'anonymisation",
            description = "Réservé aux administrateurs. Remplace les données personnelles du demandeur par des valeurs anonymisées ; il ne pourra plus se connecter avec ce compte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande validée et données anonymisées"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    AnonymizationDemandResponseDto validate(@Parameter(description = "Identifiant de la demande", required = true) Long id) throws HttpException;
}
