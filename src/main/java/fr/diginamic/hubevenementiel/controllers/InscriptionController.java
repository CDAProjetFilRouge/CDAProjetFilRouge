package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionEventResponseDto;
import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionResponseDto;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.InscriptionMapper;
import fr.diginamic.hubevenementiel.openapi.InscriptionApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.InscriptionService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController implements InscriptionApi {

    private final InscriptionService inscriptionService;
    private final InscriptionMapper inscriptionMapper;

    public InscriptionController(InscriptionService inscriptionService, InscriptionMapper inscriptionMapper) {
        this.inscriptionService = inscriptionService;
        this.inscriptionMapper = inscriptionMapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<InscriptionResponseDto> register(@RequestParam Long userId, @RequestParam Long eventId)
            throws HttpException {
        Inscription inscription = inscriptionService.register(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscriptionMapper.toDto(inscription));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelByMember(@PathVariable Long id) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        inscriptionService.cancelByMember(id, principal);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @DeleteMapping("/{id}/organizer")
    public ResponseEntity<Void> cancelByOrganizer(@PathVariable Long id, @RequestParam String motif)
            throws HttpException {
        inscriptionService.cancelByOrganizer(id, motif);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/event/{eventId}")
    public List<InscriptionEventResponseDto> getByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws HttpException {
        return inscriptionService.findByEvent(eventId, page, size).stream()
                .map(inscriptionMapper::toEventDto)
                .toList();
    }

    @Override
    @GetMapping("/user/{userId}")
    public List<InscriptionResponseDto> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws HttpException {
        return inscriptionService.findByUser(userId, page, size).stream()
                .map(inscriptionMapper::toDto)
                .toList();
    }
}
