package fr.diginamic.hubevenementiel.openapi;

import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentRequestDto;
import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentResponseDto;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Documents légaux", description = "Conditions d'utilisation et politique RGPD de la plateforme.")
@SecurityRequirement(name = "bearerAuth")
public interface LegalDocumentApi {

    @Operation(summary = "Consulter la dernière version d'un document légal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Aucune version publiée pour ce type de document")
    })
    LegalDocumentResponseDto getLatest(@Parameter(description = "Type de document (conditions d'utilisation ou politique RGPD)", required = true) DocumentType type) throws HttpException;

    @Operation(summary = "Publier une nouvelle version d'un document légal", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nouvelle version créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis")
    })
    ResponseEntity<LegalDocumentResponseDto> createNewVersion(LegalDocumentRequestDto requestDto) throws HttpException;
}
