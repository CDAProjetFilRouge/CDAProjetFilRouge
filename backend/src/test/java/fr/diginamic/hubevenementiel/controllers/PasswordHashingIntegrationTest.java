package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class PasswordHashingIntegrationTest {

    private static final String PASSWORD = "motdepasse123456";
    private static final String CURRENT_PREFIX = "$argon2id$v=19$m=19456,t=2,p=1$";
    private static final String LEGACY_PREFIX = "$argon2id$v=19$m=16384,t=2,p=1$";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppUser createUserWithHash(String hash) {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("argon2-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(hash);
        user.setRole(Role.MEMBER);
        user.setStatus(AccountStatus.ACTIVE);
        return userRepo.saveAndFlush(user);
    }

    private int login(String email, String password) throws Exception {
        return mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void newHashesUseArgon2idWithTheOwaspMinimumParameters() {
        assertThat(passwordEncoder.encode(PASSWORD)).startsWith(CURRENT_PREFIX);
    }

    @Test
    void hashFromTheFormerParametersStillMatchesAndIsFlaggedForUpgrade() {
        String legacy = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);

        assertThat(legacy).startsWith(LEGACY_PREFIX);
        assertThat(passwordEncoder.matches(PASSWORD, legacy)).isTrue();
        assertThat(passwordEncoder.upgradeEncoding(legacy)).isTrue();
        assertThat(passwordEncoder.upgradeEncoding(passwordEncoder.encode(PASSWORD))).isFalse();
    }

    @Test
    void legacyHashIsReplacedAtTheNextSuccessfulLogin() throws Exception {
        String legacy = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);
        AppUser user = createUserWithHash(legacy);

        assertThat(login(user.getEmail(), PASSWORD)).isEqualTo(200);

        String upgraded = userRepo.findById(user.getId()).orElseThrow().getHashedPassword();
        assertThat(upgraded).startsWith(CURRENT_PREFIX).isNotEqualTo(legacy);
        assertThat(login(user.getEmail(), PASSWORD)).isEqualTo(200);
    }

    @Test
    void failedLoginDoesNotTouchTheStoredHash() throws Exception {
        String legacy = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);
        AppUser user = createUserWithHash(legacy);

        assertThat(login(user.getEmail(), "mauvais-mot-de-passe")).isEqualTo(401);

        assertThat(userRepo.findById(user.getId()).orElseThrow().getHashedPassword()).isEqualTo(legacy);
    }

    @Test
    void currentHashIsLeftUntouchedAtLogin() throws Exception {
        String current = passwordEncoder.encode(PASSWORD);
        AppUser user = createUserWithHash(current);

        assertThat(login(user.getEmail(), PASSWORD)).isEqualTo(200);

        assertThat(userRepo.findById(user.getId()).orElseThrow().getHashedPassword()).isEqualTo(current);
    }
}
