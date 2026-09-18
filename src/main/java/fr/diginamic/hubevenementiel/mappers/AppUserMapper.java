package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.appUser.*;
import fr.diginamic.hubevenementiel.entities.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AppUserMapper {

    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private ClubSummaryMapper clubSummaryMapper;

    public AppUserResponseDto toDto(AppUser user) {
        if (user == null) {
            return null;
        }

        AppUserResponseDto dto = new AppUserResponseDto();
        dto.setId(user.getId());
        dto.setLastName(user.getLastName());
        dto.setFirstName(user.getFirstName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setSuspensionEndDate(user.getSuspensionEndDate());
        dto.setCreationDate(user.getCreationDate());
        dto.setAddress(addressMapper.toDto(user.getAddress()));
        dto.setClubs(user.getClubs().stream().map(clubSummaryMapper::toDto).collect(Collectors.toList()));

        return dto;
    }

    public AppUser toEntity(AppUserRequestDto requestDto) {
        AppUser entity = new AppUser();
        entity.setFirstName(requestDto.getFirstName());
        entity.setLastName(requestDto.getLastName());
        entity.setEmail(requestDto.getEmail());
        entity.setPhone(requestDto.getPhone());
        entity.setHashedPassword(requestDto.getPassword());
        if (requestDto.getAddress() != null) {
            entity.setAddress(addressMapper.toEntity(requestDto.getAddress()));
        }

        return entity;
    }

    public AppUser toEntityForAdminUpdate(AppUserAdminUpdateRequestDto dto) {
        AppUser entity = new AppUser();
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setRole(dto.getRole());
        if (dto.getAddress() != null) {
            entity.setAddress(addressMapper.toEntity(dto.getAddress()));
        }

        return entity;
    }

    public void updateEntityFromDto(AppUserUpdateRequestDto dto, AppUser user) {
        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAddress() != null) {
            user.setAddress(addressMapper.toEntity(dto.getAddress()));
        }

    }

    public AppUser toEntityForAdminCreate(AppUserAdminCreateRequestDto dto) {
        AppUser entity = new AppUser();
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setRole(dto.getRole());

        return entity;
    }
}
