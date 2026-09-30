package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.image.ImageSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Image;
import org.springframework.stereotype.Component;

@Component
public class ImageGaleryMapper {

    public ImageSummaryResponseDto toDto(Image image) {
        if (image == null) {
            return null;
        }

        ImageSummaryResponseDto dto = new ImageSummaryResponseDto();
        dto.setId(image.getId());
        dto.setPath(image.getPath());
        dto.setDisplayOrder(image.getDisplayOrder());

        return dto;
    }
}
