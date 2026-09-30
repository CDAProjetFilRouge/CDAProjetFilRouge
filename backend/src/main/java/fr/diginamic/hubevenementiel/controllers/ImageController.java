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
import org.springframework.web.multipart.MultipartFile;

import fr.diginamic.hubevenementiel.dtos.image.ImageSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.ImageGaleryMapper;
import fr.diginamic.hubevenementiel.openapi.ImageApi;
import fr.diginamic.hubevenementiel.services.ImageService;

@RestController
@RequestMapping("/events/{eventId}/images")
public class ImageController implements ImageApi {

    private final ImageService imageService;
    private final ImageGaleryMapper imageGaleryMapper;

    public ImageController(ImageService imageService, ImageGaleryMapper imageGaleryMapper) {
        this.imageService = imageService;
        this.imageGaleryMapper = imageGaleryMapper;
    }

    @Override
    @GetMapping
    public List<ImageSummaryResponseDto> getByEvent(@PathVariable Long eventId) throws HttpException {
        return imageService.findByEvent(eventId).stream()
                .map(imageGaleryMapper::toDto)
                .toList();
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @PostMapping
    public ResponseEntity<ImageSummaryResponseDto> upload(@PathVariable Long eventId, @RequestParam MultipartFile file)
            throws HttpException {
        Image created = imageService.upload(eventId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(imageGaleryMapper.toDto(created));
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long id) throws HttpException {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
