package fr.diginamic.hubevenementiel.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.AppUserService;

@RestController
@RequestMapping("/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<AppUser> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appUserService.findAllUsers(page, size);
    }

    @GetMapping("/{id}")
    public AppUser getById(@PathVariable Long id) throws HttpException {
        return appUserService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody AppUser user) throws HttpException {
        appUserService.createAccount(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/admin")
    public ResponseEntity<Void> createByAdmin(@RequestBody AppUser user, @RequestParam Role role)
            throws HttpException {
        appUserService.createAccountByAdmin(user, role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateOwnAccount(@PathVariable Long id, @RequestBody AppUser user)
            throws HttpException {
        appUserService.updateOwnAccount(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<Void> updateByAdmin(@PathVariable Long id, @RequestBody AppUser user)
            throws HttpException {
        appUserService.updateAccountByAdmin(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws HttpException {
        appUserService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
