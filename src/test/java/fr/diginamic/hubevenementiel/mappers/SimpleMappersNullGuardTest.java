package fr.diginamic.hubevenementiel.mappers;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

// Un test null-guard par mapper "feuille" (pas de logique de composition riche),
// pour verifier que toDto(null) ne plante jamais.
class SimpleMappersNullGuardTest {

    @Test
    void addressMapper_toDto_null_returnsNull() {
        assertThat(new AddressMapper().toDto(null)).isNull();
    }

    @Test
    void clubSummaryMapper_toDto_null_returnsNull() {
        assertThat(new ClubSummaryMapper().toDto(null)).isNull();
    }

    @Test
    void commentMapper_toDto_null_returnsNull() {
        CommentMapper mapper = new CommentMapper();
        ReflectionTestUtils.setField(mapper, "appUserSummaryMapper", new AppUserSummaryMapper());
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void appUserSummaryMapper_toDto_null_returnsNull() {
        assertThat(new AppUserSummaryMapper().toDto(null)).isNull();
    }

    @Test
    void imageGaleryMapper_toDto_null_returnsNull() {
        assertThat(new ImageGaleryMapper().toDto(null)).isNull();
    }

    @Test
    void legalDocumentMapper_toDto_null_returnsNull() {
        LegalDocumentMapper mapper = new LegalDocumentMapper();
        ReflectionTestUtils.setField(mapper, "appUserSummaryMapper", new AppUserSummaryMapper());
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void anonymisationDemandMapper_toDto_null_returnsNull() {
        AnonymisationDemandMapper mapper = new AnonymisationDemandMapper();
        ReflectionTestUtils.setField(mapper, "appUserSummaryMapper", new AppUserSummaryMapper());
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void eventSummaryMapper_toDto_null_returnsNull() {
        assertThat(new EventSummaryMapper().toDto(null)).isNull();
    }
}
