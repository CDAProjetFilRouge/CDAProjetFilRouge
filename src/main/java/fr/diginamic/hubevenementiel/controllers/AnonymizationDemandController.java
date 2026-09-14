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

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.AnonymizationDemandService;

@RestController
@RequestMapping("/anonymization-demands")
public class AnonymizationDemandController {

    private final AnonymizationDemandService anonymizationDemandService;

    public AnonymizationDemandController(AnonymizationDemandService anonymizationDemandService) {
        this.anonymizationDemandService = anonymizationDemandService;
    }

    @GetMapping
    public List<AnonymizationDemand> getDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) RequestStatus status) {
        if (status != null) {
            return anonymizationDemandService.findByStatus(page, size, status);
        }
        return anonymizationDemandService.findAll(page, size);
    }

    @PostMapping
    public ResponseEntity<AnonymizationDemand> request(@RequestParam Long requesterId) throws HttpException {
        AnonymizationDemand created = anonymizationDemandService.request(requesterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/validate")
    public AnonymizationDemand validate(@PathVariable Long id, @RequestParam Long adminId) throws HttpException {
        return anonymizationDemandService.validate(id, adminId);
    }
}
