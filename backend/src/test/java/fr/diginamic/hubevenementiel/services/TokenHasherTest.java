package fr.diginamic.hubevenementiel.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void producesTheKnownSha256OfAValue() {
        assertThat(TokenHasher.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void isStableAndAlways64Characters() {
        assertThat(TokenHasher.sha256Hex("une-valeur")).isEqualTo(TokenHasher.sha256Hex("une-valeur")).hasSize(64);
    }

    @Test
    void differentValuesGiveDifferentHashes() {
        assertThat(TokenHasher.sha256Hex("a")).isNotEqualTo(TokenHasher.sha256Hex("b"));
    }
}
