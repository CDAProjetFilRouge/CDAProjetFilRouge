package fr.diginamic.hubevenementiel.controllers;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.services.ImageService;
import fr.diginamic.hubevenementiel.services.RemoteImageStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/internal/uploads")
public class ImageStorageController {

    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.[A-Za-z0-9]{1,10})?$");

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.image-storage.key:}")
    private String storageKey;

    @PutMapping("/{name}")
    public ResponseEntity<Void> store(@PathVariable String name,
                                      @RequestHeader(value = RemoteImageStorage.KEY_HEADER, required = false) String key,
                                      HttpServletRequest request) throws HttpException, IOException {
        checkAccess(name, key);

        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Le fichier doit être une image.");
        }

        byte[] content = request.getInputStream().readNBytes((int) ImageService.MAX_FILE_SIZE_BYTES + 1);
        if (content.length == 0) {
            throw new BadRequestException("Le fichier ne peut pas être vide.");
        }
        if (content.length > ImageService.MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("L'image ne peut pas dépasser 5 Mo.");
        }

        Path targetDir = Path.of(uploadDir);
        Files.createDirectories(targetDir);
        try {
            Files.write(targetDir.resolve(name), content, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            throw new ConflictException("Ce fichier existe déjà.");
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> remove(@PathVariable String name,
                                       @RequestHeader(value = RemoteImageStorage.KEY_HEADER, required = false) String key)
            throws HttpException, IOException {
        checkAccess(name, key);
        if (!Files.deleteIfExists(Path.of(uploadDir).resolve(name))) {
            throw new NotFoundException("Fichier introuvable.");
        }
        return ResponseEntity.noContent().build();
    }

    private void checkAccess(String name, String key) throws HttpException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new NotFoundException("Not found");
        }
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), storageKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Clé de stockage invalide.");
        }
        if (!STORED_NAME.matcher(name).matches()) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
    }
}
