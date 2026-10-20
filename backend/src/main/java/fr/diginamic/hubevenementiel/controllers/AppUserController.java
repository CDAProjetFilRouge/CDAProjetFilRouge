package fr.diginamic.hubevenementiel.controllers;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.appUser.AppUserAdminCreateRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserAdminUpdateRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserResponseDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSuspensionRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserUpdateRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.PageResponseDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.AppUserMapper;
import fr.diginamic.hubevenementiel.mappers.AppUserSummaryMapper;
import fr.diginamic.hubevenementiel.openapi.AppUserApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.AccountDeletionService;
import fr.diginamic.hubevenementiel.services.AccountStatusService;
import fr.diginamic.hubevenementiel.services.AppUserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class AppUserController implements AppUserApi {

    private final AppUserService appUserService;
    private final AccountStatusService accountStatusService;
    private final AccountDeletionService accountDeletionService;
    private final AppUserMapper appUserMapper;
    private final AppUserSummaryMapper appUserSummaryMapper;

    public AppUserController(AppUserService appUserService, AccountStatusService accountStatusService,
            AccountDeletionService accountDeletionService, AppUserMapper appUserMapper,
            AppUserSummaryMapper appUserSummaryMapper) {
        this.appUserService = appUserService;
        this.accountStatusService = accountStatusService;
        this.accountDeletionService = accountDeletionService;
        this.appUserMapper = appUserMapper;
        this.appUserSummaryMapper = appUserSummaryMapper;
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @GetMapping
    public PageResponseDto<AppUserResponseDto> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status) {
        Page<AppUser> result = appUserService.searchUsers(q, role, status, page, size);
        List<AppUserResponseDto> content = result.getContent().stream()
                .map(appUserMapper::toDto)
                .toList();
        return new PageResponseDto<>(
            content,
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize());
    }

    @Override
    @GetMapping("/me")
    public AppUserResponseDto getMe() throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        return appUserMapper.toDto(appUserService.findById(principal.id()));
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @GetMapping("/{id}")
    public AppUserResponseDto getById(@PathVariable UUID id) throws HttpException {
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
    public ResponseEntity<AppUserResponseDto> createByAdmin(@Valid @RequestBody AppUserAdminCreateRequestDto requestDto)
            throws HttpException {
        AppUser user = appUserMapper.toEntityForAdminCreate(requestDto);
        AppUser created = appUserService.createAccountByAdmin(user, requestDto.getRole(), requestDto.getClubIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserMapper.toDto(created));
    }

    
    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}/suspend")
    public AppUserResponseDto suspendAccount(@PathVariable UUID id, @RequestBody(required = false) AppUserSuspensionRequestDto requestDto) throws HttpException {
        LocalDateTime suspenstionEndDate = null;
        if (requestDto != null) {
            suspenstionEndDate = requestDto.getSuspensionEndDate();            
        }
        
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        accountStatusService.suspend(id, suspenstionEndDate, principal.id());
        AppUser suspendedAppUser = appUserService.findById(id);
        return appUserMapper.toDto(suspendedAppUser); 
    }

    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}/reactivate")
    public AppUserResponseDto reactivateAccount(@PathVariable UUID id) throws HttpException {
        accountStatusService.reactivate(id);
        AppUser reactivatedAppUser = appUserService.findById(id);
        return appUserMapper.toDto(reactivatedAppUser);
    }

    @Override
    @PutMapping("/{id}")
    public AppUserResponseDto updateOwnAccount(@PathVariable UUID id, @RequestBody AppUserUpdateRequestDto requestDto)
            throws HttpException {
        AppUser modifiedUser = new AppUser();
        appUserMapper.updateEntityFromDto(requestDto, modifiedUser);
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        AppUser updated = appUserService.updateOwnAccount(id, modifiedUser, principal);
        return appUserMapper.toDto(updated);
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @PutMapping("/{id}/admin")
    public AppUserResponseDto updateByAdmin(@PathVariable UUID id, @RequestBody AppUserAdminUpdateRequestDto requestDto)
            throws HttpException {
        AppUser modifiedUser = appUserMapper.toEntityForAdminUpdate(requestDto);
        AppUser updated = appUserService.updateAccountByAdmin(id, modifiedUser, requestDto.getClubIds());
        return appUserMapper.toDto(updated);
    }

    @Override
    @Secured("ROLE_ADMINISTRATOR")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean deleteComments) throws HttpException {
        accountDeletionService.deleteAccount(id, deleteComments);
        return ResponseEntity.noContent().build();
    }
}
