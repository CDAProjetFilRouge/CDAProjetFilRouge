package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.appUser.AppUserAdminUpdateRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserResponseDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserUpdateRequestDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.AppUserMapper;
import fr.diginamic.hubevenementiel.mappers.AppUserSummaryMapper;
import fr.diginamic.hubevenementiel.openapi.AppUserApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.AppUserService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/users")
public class AppUserController implements AppUserApi {

    private final AppUserService appUserService;
    private final AppUserMapper appUserMapper;
    private final AppUserSummaryMapper appUserSummaryMapper;

    public AppUserController(AppUserService appUserService, AppUserMapper appUserMapper,
            AppUserSummaryMapper appUserSummaryMapper) {
        this.appUserService = appUserService;
        this.appUserMapper = appUserMapper;
        this.appUserSummaryMapper = appUserSummaryMapper;
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @GetMapping
    public List<AppUserSummaryResponseDto> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appUserService.findAllUsers(page, size).stream()
                .map(appUserSummaryMapper::toDto)
                .toList();
    }

    @Override
    @GetMapping("/{id}")
    public AppUserResponseDto getById(@PathVariable Long id) throws HttpException {
        return appUserMapper.toDto(appUserService.findById(id));
    }

    @Override
    @PostMapping
    public ResponseEntity<AppUserResponseDto> register(@RequestBody AppUserRequestDto requestDto) throws HttpException {
        AppUser user = appUserMapper.toEntity(requestDto);
        AppUser created = appUserService.createAccount(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserMapper.toDto(created));
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PostMapping("/admin")
    public ResponseEntity<AppUserResponseDto> createByAdmin(@RequestBody AppUserRequestDto requestDto,
            @RequestParam Role role) throws HttpException {
        AppUser user = appUserMapper.toEntity(requestDto);
        AppUser created = appUserService.createAccountByAdmin(user, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserMapper.toDto(created));
    }

    @Override
    @PutMapping("/{id}")
    public AppUserResponseDto updateOwnAccount(@PathVariable Long id, @RequestBody AppUserUpdateRequestDto requestDto)
            throws HttpException {
        AppUser modifiedUser = new AppUser();
        appUserMapper.updateEntityFromDto(requestDto, modifiedUser);
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser updated = appUserService.updateOwnAccount(id, modifiedUser, principal);
        return appUserMapper.toDto(updated);
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}/admin")
    public AppUserResponseDto updateByAdmin(@PathVariable Long id, @RequestBody AppUserAdminUpdateRequestDto requestDto)
            throws HttpException {
        AppUser modifiedUser = appUserMapper.toEntityForAdminUpdate(requestDto);
        AppUser updated = appUserService.updateAccountByAdmin(id, modifiedUser, requestDto.getClubIds());
        return appUserMapper.toDto(updated);
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws HttpException {
        appUserService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
