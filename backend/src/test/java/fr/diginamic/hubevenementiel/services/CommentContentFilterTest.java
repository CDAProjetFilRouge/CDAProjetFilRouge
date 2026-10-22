package fr.diginamic.hubevenementiel.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;

class CommentContentFilterTest {

    private final CommentContentFilter filter = new CommentContentFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "Super soirée, merci à toute l'équipe !",
            "Un très beau concert, la réputation du club n'est plus à faire.",
            "Rendez-vous l'an prochain, c'était génial (50 personnes, 3 heures).",
            "Bravo à tous, quelle ambiance : MERCI",
            "Le parcours se termine en cul-de-sac, prévoyez la navette.",
            "Quelle bite d'amarrage solide sur le ponton !" })
    void check_acceptsOrdinaryComments(String content) {
        assertDoesNotThrow(() -> filter.check(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Allez voir https://exemple.com/promo",
            "Plus d'infos sur www.exemple.fr",
            "Gagnez gros sur casino.xyz",
            "Tu es un connard",
            "quel CONNAAARD",
            "Espèce d'abruti",
            "c0nnard",
            "ntm",
            "Espèce de fils de pute",
            "Nique  ta   mère",
            "sale bougnoule" })
    void check_refusesLinksAndInsults(String content) {
        assertThrows(BadRequestException.class, () -> filter.check(content));
    }

    @Test
    void check_refusesBlankTooLongFloodAndShouting() {
        assertThrows(BadRequestException.class, () -> filter.check("   "));
        assertThrows(BadRequestException.class, () -> filter.check(null));
        assertThrows(BadRequestException.class, () -> filter.check("a".repeat(CommentContentFilter.MAX_LENGTH + 1)));
        assertThrows(BadRequestException.class, () -> filter.check("Génial !!!!!!!!!!!!"));
        assertThrows(BadRequestException.class, () -> filter.check("VENEZ TOUS ICI C'EST VRAIMENT SUPER"));
    }
}
