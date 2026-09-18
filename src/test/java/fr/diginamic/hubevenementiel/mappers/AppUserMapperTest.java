package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.appUser.AppUserRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserResponseDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserUpdateRequestDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserMapperTest {

    private AppUserMapper appUserMapper;

    @BeforeEach
    void setUp() {
        appUserMapper = new AppUserMapper();
        ReflectionTestUtils.setField(appUserMapper, "addressMapper", new AddressMapper());
        ReflectionTestUtils.setField(appUserMapper, "clubSummaryMapper", new ClubSummaryMapper());
    }

    @Test
    void toDto_nullUser_returnsNull() {
        assertThat(appUserMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_emptyClubs_returnsEmptyListNotNPE() {
        AppUser user = new AppUser();
        user.setClubs(List.of());

        AppUserResponseDto dto = appUserMapper.toDto(user);

        assertThat(dto.getClubs()).isEmpty();
    }

    @Test
    void toEntity_missingAddress_doesNotThrow() {
        AppUserRequestDto dto = new AppUserRequestDto();
        dto.setAddress(null);
        dto.setPassword("motdepasse12");

        AppUser entity = appUserMapper.toEntity(dto);

        assertThat(entity.getAddress()).isNull();
    }

    @Test
    void toEntity_passwordCopiedToHashedPassword() {
        AppUserRequestDto dto = new AppUserRequestDto();
        dto.setPassword("motdepasse12");

        AppUser entity = appUserMapper.toEntity(dto);

        assertThat(entity.getHashedPassword()).isEqualTo("motdepasse12");
    }

    @Test
    void updateEntityFromDto_nullFieldLeavesExistingValueIntact() {
        AppUser user = new AppUser();
        user.setFirstName("Alice");
        user.setEmail("alice@example.com");

        AppUserUpdateRequestDto dto = new AppUserUpdateRequestDto();
        dto.setFirstName(null);
        dto.setEmail("nouvel-email@example.com");

        appUserMapper.updateEntityFromDto(dto, user);

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("nouvel-email@example.com");
    }

    @Test
    void updateEntityFromDto_allFieldsProvided_allOverwritten() {
        AppUser user = new AppUser();
        user.setFirstName("Ancien");
        user.setLastName("Nom");
        user.setEmail("ancien@example.com");
        user.setPhone("0000000000");

        AppUserUpdateRequestDto dto = new AppUserUpdateRequestDto();
        dto.setFirstName("Nouveau");
        dto.setLastName("Prenom");
        dto.setEmail("nouveau@example.com");
        dto.setPhone("0611111111");

        appUserMapper.updateEntityFromDto(dto, user);

        assertThat(user.getFirstName()).isEqualTo("Nouveau");
        assertThat(user.getLastName()).isEqualTo("Prenom");
        assertThat(user.getEmail()).isEqualTo("nouveau@example.com");
        assertThat(user.getPhone()).isEqualTo("0611111111");
    }
}
