package fr.diginamic.hubevenementiel.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;

/**
 * Refuse les commentaires qui ressemblent à du spam ou à des insultes courantes. Filtre volontairement modeste :
 * il écarte les cas évidents, la modération fine reste à l'organisateur et aux administrateurs (suppression).
 */
@Component
public class CommentContentFilter {

    public static final int MAX_LENGTH = 1000;

    private static final Pattern LINK = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(com|fr|net|org|io|ru|xyz|info|biz|ly|co|cc|tk|top)\\b)");
    private static final Pattern FLOOD = Pattern.compile("(.)\\1{7,}");
    private static final Pattern REPEATED_LETTERS = Pattern.compile("(\\p{L})\\1+");

    private static final String BANNED_WORDS_FILE = "comment-banned-words.txt";

    // Liste modifiable dans src/main/resources/comment-banned-words.txt, normalisée comme le texte contrôlé.
    private final Set<String> bannedWords = loadBannedWords();

    public void check(String content) throws BadRequestException {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Le commentaire ne peut pas être vide.");
        }
        if (content.length() > MAX_LENGTH) {
            throw new BadRequestException("Le commentaire ne peut pas dépasser " + MAX_LENGTH + " caractères.");
        }
        if (LINK.matcher(content).find()) {
            throw new BadRequestException("Les liens ne sont pas autorisés dans les commentaires.");
        }
        if (FLOOD.matcher(content).find()) {
            throw new BadRequestException("Le commentaire contient trop de caractères répétés.");
        }
        if (isShouting(content)) {
            throw new BadRequestException("Merci de ne pas écrire tout en majuscules.");
        }
        String padded = " " + normalize(content) + " ";
        for (String banned : bannedWords) {
            if (padded.contains(" " + banned + " ")) {
                throw new BadRequestException("Ce commentaire contient des termes injurieux.");
            }
        }
    }

    private static Set<String> loadBannedWords() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(BANNED_WORDS_FILE).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(CommentContentFilter::normalize)
                    .filter(word -> !word.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Liste de mots interdits illisible : " + BANNED_WORDS_FILE, e);
        }
    }

    private static boolean isShouting(String content) {
        long letters = content.chars().filter(Character::isLetter).count();
        long upper = content.chars().filter(Character::isUpperCase).count();
        return letters >= 15 && upper * 10 > letters * 7;
    }

    // Minuscules, sans accents, chiffres usuels remplacés par leur lettre, ponctuation retirée, lettres répétées réduites.
    static String normalize(String text) {
        String lower = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String leet = lower.replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('5', 's')
                .replace('@', 'a').replace('$', 's');
        String letters = leet.replaceAll("[^\\p{L}]+", " ").trim();
        return REPEATED_LETTERS.matcher(letters).replaceAll("$1");
    }
}
