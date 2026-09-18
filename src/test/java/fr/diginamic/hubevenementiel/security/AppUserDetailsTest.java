package fr.diginamic.hubevenementiel.security;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserDetailsTest {

    @Test
    void isEnabled_activeStatus_returnsTrue() {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.ACTIVE);

        assertThat(new AppUserDetails(user).isEnabled()).isTrue();
    }

    @Test
    void isEnabled_inactiveStatus_returnsFalse() {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.INACTIVE);

        assertThat(new AppUserDetails(user).isEnabled()).isFalse();
    }

    @Test
    void isEnabled_suspendedStatus_returnsFalse() {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.SUSPENDED);

        assertThat(new AppUserDetails(user).isEnabled()).isFalse();
    }

    @Test
    void isEnabled_anonymizeStatus_returnsFalse() {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.ANONYMIZE);

        assertThat(new AppUserDetails(user).isEnabled()).isFalse();
    }

    @Test
    void getAuthorities_returnsRolePrefixedUppercase() {
        AppUser user = new AppUser();
        user.setRole(Role.ADMINISTRATOR);

        List<? extends GrantedAuthority> authorities = List.copyOf(new AppUserDetails(user).getAuthorities());

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_ADMINISTRATOR");
    }
}
