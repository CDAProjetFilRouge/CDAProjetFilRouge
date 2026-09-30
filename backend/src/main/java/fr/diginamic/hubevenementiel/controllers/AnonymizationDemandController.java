package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.anonymizerDemand.AnonymizationDemandResponseDto;
import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.AnonymisationDemandMapper;
import fr.diginamic.hubevenementiel.openapi.AnonymizationDemandApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.AnonymizationDemandService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/anonymization-demands")
public class AnonymizationDemandController implements AnonymizationDemandApi {

    private final AnonymizationDemandService anonymizationDemandService;
    private final AnonymisationDemandMapper anonymizationDemandMapper;

    public AnonymizationDemandController(AnonymizationDemandService anonymizationDemandService,
            AnonymisationDemandMapper anonymizationDemandMapper) {
        this.anonymizationDemandService = anonymizationDemandService;
        this.anonymizationDemandMapper = anonymizationDemandMapper;
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @GetMapping
    public List<AnonymizationDemandResponseDto> getDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) RequestStatus status) throws HttpException {
        return anonymizationDemandService.search(page, size, status).stream()
                .map(anonymizationDemandMapper::toDto)
                .toList();
    }

    @Override
    @PostMapping
    public ResponseEntity<AnonymizationDemandResponseDto> request() throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AnonymizationDemand created = anonymizationDemandService.request(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(anonymizationDemandMapper.toDto(created));
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}/validate")
    public AnonymizationDemandResponseDto validate(@PathVariable Long id) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AnonymizationDemand validated = anonymizationDemandService.validate(id, principal);
        return anonymizationDemandMapper.toDto(validated);
    }
}
