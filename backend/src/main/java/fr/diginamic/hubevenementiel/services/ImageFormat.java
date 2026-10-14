package fr.diginamic.hubevenementiel.services;

import java.util.Optional;

/**
 * Formats d'image acceptés au téléversement : uniquement des formats raster, reconnus à leur contenu réel
 * (signature en tête de fichier) et jamais au type annoncé par le client ni à l'extension du nom. Un SVG, qui
 * peut contenir du script, un HTML ou un exécutable déguisé en image sont donc refusés.
 */
public enum ImageFormat {

    JPEG("image/jpeg", ".jpg"),
    PNG("image/png", ".png"),
    WEBP("image/webp", ".webp");

    private final String mimeType;
    private final String extension;

    ImageFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    /**
     * @param content contenu du fichier (seuls les premiers octets sont lus)
     * @return le format reconnu, ou vide si le contenu n'est ni du JPEG, ni du PNG, ni du WebP
     */
    public static Optional<ImageFormat> detect(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        if (startsWith(content, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(content, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        // WebP : conteneur RIFF, « WEBP » aux octets 8 à 11.
        if (startsWith(content, 0, 0x52, 0x49, 0x46, 0x46) && startsWith(content, 8, 0x57, 0x45, 0x42, 0x50)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, int... signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
