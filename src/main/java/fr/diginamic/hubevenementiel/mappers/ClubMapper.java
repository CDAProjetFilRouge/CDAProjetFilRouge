package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.club.ClubRequestDto;
import fr.diginamic.hubevenementiel.dtos.club.ClubResponseDto;
import fr.diginamic.hubevenementiel.entities.Club;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ClubMapper {

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;

    public ClubResponseDto toDto(Club club) {
        if (club == null) {
            return null;
        }

        ClubResponseDto dto = new ClubResponseDto();
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setCategory(club.getCategory());
        dto.setEmail(club.getEmail());
        dto.setPhone(club.getPhone());
        dto.setEndValidityDate(club.getEndValidityDate());
        dto.setAddress(addressMapper.toDto(club.getAddress()));
        dto.setAppUsers(club.getAppUsers().stream().map(appUserSummaryMapper::toDto).collect(Collectors.toList()));

        return dto;
    }

    public Club toEntity(ClubRequestDto dto) {
        Club entity = new Club();
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setEndValidityDate(dto.getEndValidityDate());
        if (dto.getAddress() != null) {
            entity.setAddress(addressMapper.toEntity(dto.getAddress()));
        }

        return entity;
    }
}
