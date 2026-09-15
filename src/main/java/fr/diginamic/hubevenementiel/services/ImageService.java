package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ImageRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepo imageRepo;
    private final EventService eventService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ImageService(ImageRepo imageRepo, EventService eventService) {
        this.imageRepo = imageRepo;
        this.eventService = eventService;
    }

    // Non utilisée par le controller, remplacée par findByEvent(). Désactivée pour
    // éviter un doublon de logique.
    // public List<Image> getAllImage(){
    // return imageRepo.findAll();
    // }

    public List<Image> findByEvent(Long eventId) throws HttpException {
        eventService.findById(eventId);

        return imageRepo.findByEventIdOrderByDisplayOrderAsc(eventId);
    }

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGES_PER_EVENT = 10;

    public boolean imageChecker(Long eventId, MultipartFile file) throws HttpException {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier ne peut pas être vide.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new BadRequestException("Le fichier doit être une image.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("L'image ne peut pas dépasser 5 Mo.");
        }

        if (imageRepo.countByEventId(eventId) >= MAX_IMAGES_PER_EVENT) {
            throw new BadRequestException("Un évènement ne peut pas avoir plus de 10 images.");
        }

        return true;
    }

    @Transactional
    public Image upload(Long eventId, MultipartFile file) throws HttpException {
        Event event = eventService.findById(eventId);

        imageChecker(eventId, file);

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }
        String storedName = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadDir);
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer le fichier.", e);
        }

        long displayOrder = imageRepo.countByEventId(eventId);

        Image image = new Image();
        image.setFileName(originalName);
        image.setPath("/uploads/" + storedName);
        image.setMimeType(file.getContentType());
        image.setSizeByte(file.getSize());
        image.setUploadDate(LocalDateTime.now());
        image.setDisplayOrder((int) displayOrder);
        image.setEvent(event);

        return imageRepo.save(image);
    }

    @Transactional
    public void delete(Long id) throws HttpException {
        Image image = getImageById(id);

        try {
            Files.deleteIfExists(Path.of(uploadDir, image.getPath().replace("/uploads/", "")));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de supprimer le fichier.", e);
        }

        imageRepo.delete(image);
    }

    public Image getImageById(Long id) throws HttpException {
        Optional<Image> i = imageRepo.findById(id);

        if (i.isEmpty()) {
            throw new NotFoundException("No image found with id: " + id);
        }

        return i.get();
    }

    // Non utilisée par le controller, remplacée par upload() (gère aussi l'écriture
    // du fichier). Désactivée pour éviter un doublon de logique.
    // @Transactional
    // public void createImage(Image image){
    // imageRepo.save(image);
    // }

    // Désactivée : aucun champ de cette entité n'a de raison d'être modifié
    // librement une fois l'image
    // uploadée (fileName/path/mimeType/sizeByte décrivent le fichier réellement
    // écrit sur disque,
    // uploadDate est posée à la création, event ne doit pas changer). À réactiver
    // seulement si un
    // vrai besoin apparaît (ex: réordonner displayOrder), avec une liste de champs
    // restreinte.
    // @Transactional
    // public void updateImage(Image image) throws HttpException {
    // Optional<Image> i = imageRepo.findById(image.getId());
    // if(i.isEmpty()){
    // throw new NotFoundException("No image found with this id: "+image.getId());
    // }
    //
    // i.get().setFileName(image.getFileName());
    // i.get().setPath(image.getPath());
    // i.get().setMimeType(image.getMimeType());
    // i.get().setSizeByte(image.getSizeByte());
    // i.get().setUploadDate(image.getUploadDate());
    // i.get().setDisplayOrder(image.getDisplayOrder());
    // i.get().setEvent(image.getEvent());
    // }
}
