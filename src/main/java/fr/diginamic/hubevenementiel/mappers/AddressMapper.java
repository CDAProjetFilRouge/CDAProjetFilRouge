package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.dtos.address.AddressResponseDto;
import fr.diginamic.hubevenementiel.entities.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponseDto toDto(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponseDto dto = new AddressResponseDto();
        dto.setId(address.getId());
        dto.setStreet1(address.getStreet1());
        dto.setStreet2(address.getStreet2());
        dto.setPostalCode(address.getPostalCode());
        dto.setCity(address.getCity());
        dto.setCountry(address.getCountry());

        return dto;
    }

    public Address toEntity(AddressRequestDto requestDto) {
        Address entity = new Address();
        entity.setStreet1(requestDto.getStreet1());
        entity.setStreet2(requestDto.getStreet2());
        entity.setPostalCode(requestDto.getPostalCode());
        entity.setCity(requestDto.getCity());
        entity.setCountry(requestDto.getCountry());

        return entity;
    }
}
