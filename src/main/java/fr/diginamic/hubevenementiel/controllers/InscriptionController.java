package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.InscriptionService;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    public InscriptionController(InscriptionService inscriptionService) {
        this.inscriptionService = inscriptionService;
    }

    @PostMapping
    public ResponseEntity<Inscription> register(@RequestParam Long userId, @RequestParam Long eventId)
            throws HttpException {
        Inscription inscription = inscriptionService.register(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscription);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelByMember(@PathVariable Long id) throws HttpException {
        inscriptionService.cancelByMember(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/organizer")
    public ResponseEntity<Void> cancelByOrganizer(@PathVariable Long id, @RequestParam String motif)
            throws HttpException {
        inscriptionService.cancelByOrganizer(id, motif);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/event/{eventId}")
    public List<Inscription> getByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inscriptionService.findByEvent(eventId, page, size);
    }

    @GetMapping("/user/{userId}")
    public List<Inscription> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inscriptionService.findByUser(userId, page, size);
    }
}
