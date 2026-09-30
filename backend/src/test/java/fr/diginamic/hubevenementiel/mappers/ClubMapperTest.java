package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.club.ClubRequestDto;
import fr.diginamic.hubevenementiel.entities.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ClubMapperTest {

    private ClubMapper clubMapper;

    @BeforeEach
    void setUp() {
        clubMapper = new ClubMapper();
        ReflectionTestUtils.setField(clubMapper, "addressMapper", new AddressMapper());
        ReflectionTestUtils.setField(clubMapper, "appUserSummaryMapper", new AppUserSummaryMapper());
    }

    @Test
    void toDto_nullClub_returnsNull() {
        assertThat(clubMapper.toDto(null)).isNull();
    }

    @Test
    void toEntity_missingAddress_doesNotThrow() {
        ClubRequestDto dto = new ClubRequestDto();
        dto.setAddress(null);

        Club entity = clubMapper.toEntity(dto);

        assertThat(entity.getAddress()).isNull();
    }
}
