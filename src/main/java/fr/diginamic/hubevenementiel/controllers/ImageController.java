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
import org.springframework.web.multipart.MultipartFile;

import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.ImageService;

@RestController
@RequestMapping("/events/{eventId}/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public List<Image> getByEvent(@PathVariable Long eventId) {
        return imageService.findByEvent(eventId);
    }

    @PostMapping
    public ResponseEntity<Image> upload(@PathVariable Long eventId, @RequestParam MultipartFile file)
            throws HttpException {
        Image created = imageService.upload(eventId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) throws HttpException {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
