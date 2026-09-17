package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import fr.diginamic.hubevenementiel.services.AnonymizationDemandService;

@RestController
@RequestMapping("/anonymization-demands")
public class AnonymizationDemandController {

    private final AnonymizationDemandService anonymizationDemandService;
    private final AnonymisationDemandMapper anonymizationDemandMapper;

    public AnonymizationDemandController(AnonymizationDemandService anonymizationDemandService,
            AnonymisationDemandMapper anonymizationDemandMapper) {
        this.anonymizationDemandService = anonymizationDemandService;
        this.anonymizationDemandMapper = anonymizationDemandMapper;
    }

    @GetMapping
    public List<AnonymizationDemandResponseDto> getDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) RequestStatus status) throws HttpException {
        return anonymizationDemandService.search(page, size, status).stream()
                .map(anonymizationDemandMapper::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AnonymizationDemandResponseDto> request(@RequestParam Long requesterId) throws HttpException {
        AnonymizationDemand created = anonymizationDemandService.request(requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(anonymizationDemandMapper.toDto(created));
    }

    @PutMapping("/{id}/validate")
    public AnonymizationDemandResponseDto validate(@PathVariable Long id, @RequestParam Long adminId)
            throws HttpException {
        AnonymizationDemand validated = anonymizationDemandService.validate(id, adminId);
        return anonymizationDemandMapper.toDto(validated);
    }
}
