package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.club.ClubRequestDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubResponseDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Clubs", description = "Gestion des clubs affiliés à la fédération.")
@SecurityRequirement(name = "bearerAuth")
public interface ClubApi {

    @Operation(summary = "Rechercher des clubs", description = "Recherche paginée, filtrable par catégorie et par ville.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des clubs"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    List<ClubSummaryResponseDto> getClubs(
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size,
            @Parameter(description = "Filtre optionnel par catégorie") Category category,
            @Parameter(description = "Filtre optionnel par ville") String city);

    @Operation(summary = "Consulter un club")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Club trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Club introuvable")
    })
    ClubResponseDto getById(@Parameter(description = "Identifiant du club", required = true) Long id) throws HttpException;

    @Operation(summary = "Créer un club", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Club créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis")
    })
    ResponseEntity<ClubResponseDto> create(ClubRequestDto requestDto) throws HttpException;

    @Operation(summary = "Modifier un club", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Club mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Club introuvable")
    })
    ClubResponseDto update(@Parameter(description = "Identifiant du club", required = true) Long id, ClubRequestDto requestDto) throws HttpException;

    @Operation(summary = "Supprimer (désaffilier) un club", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Club supprimé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Club introuvable")
    })
    ResponseEntity<Void> delete(@Parameter(description = "Identifiant du club", required = true) Long id) throws HttpException;

    @Operation(summary = "Assigne un AppUser à un Club vie leurs id respectif", description = "Réversé aux organizateurs et admins")
    @ApiResponses({
            @ApiResponse(responseCode = "205", description = "Membre assigné avec succés!"),
            @ApiResponse(responseCode = "401", description = "Non autenthifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ORGANIZER requis"),
            @ApiResponse(responseCode = "404", description = "Club ou AppUser introuvable")
    })
    ResponseEntity<Void> associateUser(@Parameter(description = "id of the Appuser we want to associate to a club", required = true) Long idUser,
                                       @Parameter(description = "id of the Club we want to associate the AppUser with", required = true) Long idClub) throws HttpException;

}
