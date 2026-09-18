package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepo clubRepository;

    @InjectMocks
    private ClubService clubService;

    private Club validClub;

    @BeforeEach
    void setUp() {
        validClub = new Club();
        validClub.setName("Club de course de Rennes");
        validClub.setCategory(Category.SPORT);
        validClub.setEmail("contact@club.fr");
        validClub.setPhone("+33 6 12 34 56 78");
        validClub.setAddress(new Address());
    }

    // ---------------------------------------------------------------
    // clubChecker
    // ---------------------------------------------------------------

    @Test
    void clubChecker_nullClub_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(null));
    }

    @Test
    void clubChecker_blankName_throwsBadRequest() {
        validClub.setName(" ");
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_nameExactly150Chars_passes() throws HttpException {
        validClub.setName("a".repeat(150));
        assertThat(clubService.clubChecker(validClub)).isTrue();
    }

    @Test
    void clubChecker_name151Chars_throwsBadRequest() {
        validClub.setName("a".repeat(151));
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_nullCategory_throwsBadRequest() {
        validClub.setCategory(null);
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_blankEmail_throwsBadRequest() {
        validClub.setEmail(" ");
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_malformedEmail_throwsBadRequest() {
        validClub.setEmail("pas-un-email");
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_blankPhone_throwsBadRequest() {
        validClub.setPhone(" ");
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_phone21Chars_throwsBadRequest() {
        validClub.setPhone("1".repeat(21));
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_phoneWithLetters_throwsBadRequest() {
        validClub.setPhone("06ABCDEFGH");
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    @Test
    void clubChecker_validPhoneFormat_passes() throws HttpException {
        validClub.setPhone("+33 6 12 34 56 78");
        assertThat(clubService.clubChecker(validClub)).isTrue();
    }

    @Test
    void clubChecker_nullAddress_throwsBadRequest() {
        validClub.setAddress(null);
        assertThrows(BadRequestException.class, () -> clubService.clubChecker(validClub));
    }

    // ---------------------------------------------------------------
    // createClub / updateClub / deleteClub
    // ---------------------------------------------------------------

    @Test
    void createClub_duplicateName_throwsConflict() {
        when(clubRepository.existsByName(validClub.getName())).thenReturn(true);

        assertThrows(ConflictException.class, () -> clubService.createClub(validClub));

        verify(clubRepository, never()).save(any());
    }

    @Test
    void createClub_happyPath_savesClub() throws HttpException {
        when(clubRepository.existsByName(validClub.getName())).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Club result = clubService.createClub(validClub);

        assertThat(result).isEqualTo(validClub);
    }

    @Test
    void updateClub_unknownId_throwsNotFound() {
        when(clubRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clubService.updateClub(1L, validClub));
    }

    @Test
    void deleteClub_unknownId_throwsNotFound() {
        when(clubRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> clubService.deleteClub(1L));
    }

    @Test
    void updateClub_nameUnchangedCaseInsensitive_doesNotCheckDuplicate() throws HttpException {
        Club existing = new Club();
        existing.setName(validClub.getName().toUpperCase());
        when(clubRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clubService.updateClub(1L, validClub);

        verify(clubRepository, never()).existsByName(anyString());
    }

    @Test
    void updateClub_nameChangedToExistingOne_throwsConflict() {
        Club existing = new Club();
        existing.setName("Autre nom");
        when(clubRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clubRepository.existsByName(validClub.getName())).thenReturn(true);

        assertThrows(ConflictException.class, () -> clubService.updateClub(1L, validClub));
    }

    // ---------------------------------------------------------------
    // RG28 : deleteClub, soft-delete
    // ---------------------------------------------------------------

    @Test
    void deleteClub_doesNotDeleteTheRow() throws HttpException {
        Club existing = new Club();
        when(clubRepository.findById(1L)).thenReturn(Optional.of(existing));

        clubService.deleteClub(1L);

        verify(clubRepository, never()).delete(any(Club.class));
    }

    @Test
    void deleteClub_setsEndValidityDateToToday() throws HttpException {
        Club existing = new Club();
        when(clubRepository.findById(1L)).thenReturn(Optional.of(existing));

        clubService.deleteClub(1L);

        assertThat(existing.getEndValidityDate()).isEqualTo(LocalDate.now());
        verify(clubRepository).save(existing);
    }
}
