package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionEventResponseDto;
import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionResponseDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class InscriptionMapperTest {

    private InscriptionMapper inscriptionMapper;

    @BeforeEach
    void setUp() {
        inscriptionMapper = new InscriptionMapper();
        ReflectionTestUtils.setField(inscriptionMapper, "eventSummaryMapper", new EventSummaryMapper());
        ReflectionTestUtils.setField(inscriptionMapper, "appUserSummaryMapper", new AppUserSummaryMapper());
    }

    @Test
    void toDto_nullInscription_returnsNull() {
        assertThat(inscriptionMapper.toDto(null)).isNull();
    }

    @Test
    void toEventDto_nullInscription_returnsNull() {
        assertThat(inscriptionMapper.toEventDto(null)).isNull();
    }

    // toDto et toEventDto exposent volontairement des faces differentes de la meme
    // inscription : toDto (cote "mes inscriptions") montre l'evenement mais pas
    // l'utilisateur (deja connu du contexte) ; toEventDto (cote "participants a mon
    // evenement") montre l'inverse. Ce test documente cette asymetrie pour eviter
    // qu'un futur refactor les fusionne par erreur.
    @Test
    void toDto_exposesEventButNotUser_toEventDto_exposesUserButNotEvent() {
        Inscription inscription = new Inscription();
        inscription.setUser(new AppUser());
        inscription.setEvent(new Event());

        InscriptionResponseDto sideView = inscriptionMapper.toDto(inscription);
        InscriptionEventResponseDto eventSideView = inscriptionMapper.toEventDto(inscription);

        assertThat(sideView.getEvent()).isNotNull();
        assertThat(eventSideView.getUser()).isNotNull();
    }
}
