package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.event.EventRequestDto;
import fr.diginamic.hubevenementiel.dtos.event.EventResponseDto;
import fr.diginamic.hubevenementiel.entities.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private EventMapper eventMapper;

    @BeforeEach
    void setUp() {
        eventMapper = new EventMapper();
        ReflectionTestUtils.setField(eventMapper, "appUserSummaryMapper", new AppUserSummaryMapper());
        ReflectionTestUtils.setField(eventMapper, "imageGaleryMapper", new ImageGaleryMapper());
        ReflectionTestUtils.setField(eventMapper, "addressMapper", new AddressMapper());
    }

    @Test
    void toDto_nullEvent_returnsNull() {
        assertThat(eventMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_emptyImageGallery_returnsEmptyList() {
        Event event = new Event();
        event.setImageGallery(List.of());

        EventResponseDto dto = eventMapper.toDto(event);

        assertThat(dto.getImageGallery()).isEmpty();
    }

    @Test
    void toEntity_missingLocation_doesNotThrow() {
        EventRequestDto dto = new EventRequestDto();
        dto.setLocation(null);

        Event entity = eventMapper.toEntity(dto);

        assertThat(entity.getLocation()).isNull();
    }
}
