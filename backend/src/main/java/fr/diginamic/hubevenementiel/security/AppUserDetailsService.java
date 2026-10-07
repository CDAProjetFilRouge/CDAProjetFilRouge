package fr.diginamic.hubevenementiel.security;

import java.time.LocalDateTime;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    public AppUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu."));

        if (appUser.getStatus() == AccountStatus.SUSPENDED
                && appUser.getSuspensionEndDate() != null
                && !appUser.getSuspensionEndDate().isAfter(LocalDateTime.now())) {
            appUser.setStatus(AccountStatus.ACTIVE);
            appUser.setSuspensionEndDate(null);
            userRepo.save(appUser);
        }

        return new AppUserDetails(appUser);
    }
}
