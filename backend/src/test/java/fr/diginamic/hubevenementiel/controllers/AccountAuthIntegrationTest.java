package fr.diginamic.hubevenementiel.controllers;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.EmailService;

// Vraie base + vrai contexte Spring. EmailService est mocke pour eviter tout appel
// SMTP reel vers Brevo pendant les tests. @Transactional fait rollback de tout a
// la fin de chaque test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AccountAuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private UserRepo userRepo;
        @Autowired
        private TokenRepo tokenRepo;
        @Autowired
        private PasswordEncoder passwordEncoder;
        @MockitoBean
        private EmailService emailService;

        private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        private static final String PASSWORD = "motdepasse123456";

        private String uniqueEmail(String prefix) {
                return prefix + "-" + System.nanoTime() + "@example.com";
        }

        private AppUser createUser(String email, AccountStatus status) {
                AppUser user = new AppUser();
                user.setFirstName("Test");
                user.setLastName("User");
                user.setEmail(email);
                user.setPhone("0600000000");
                user.setHashedPassword(passwordEncoder.encode(PASSWORD));
                user.setRole(Role.MEMBER);
                user.setStatus(status);
                return userRepo.save(user);
        }

        // ---------------------------------------------------------------
        // US-200 : Inscription (POST /users)
        // ---------------------------------------------------------------

        @Test
        void register_validPayload_returns201WithoutHashedPasswordInResponse() throws Exception {
                String email = uniqueEmail("nouveau");
                Map<String, Object> payload = Map.of(
                                "firstName", "Alice",
                                "lastName", "Martin",
                                "email", email,
                                "password", PASSWORD,
                                "phone", "0600000000");

                String response = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                org.assertj.core.api.Assertions.assertThat(response).doesNotContain("hashedPassword");
                org.assertj.core.api.Assertions.assertThat(response).doesNotContain(PASSWORD);

                AppUser stored = userRepo.findByEmail(email).orElseThrow();
                org.assertj.core.api.Assertions.assertThat(stored.getHashedPassword()).isNotEqualTo(PASSWORD);
        }

        @Test
        void register_duplicateEmail_returns409() throws Exception {
                String email = uniqueEmail("existant");
                createUser(email, AccountStatus.ACTIVE);

                Map<String, Object> payload = Map.of(
                                "firstName", "Alice",
                                "lastName", "Martin",
                                "email", email,
                                "password", PASSWORD);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isConflict());
        }

        @Test
        void register_missingRequiredField_returns400() throws Exception {
                Map<String, Object> payload = Map.of(
                                "firstName", "Alice",
                                "password", PASSWORD);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isBadRequest());
        }

        // ---------------------------------------------------------------
        // US-201 : Activation (GET /account/verify)
        // ---------------------------------------------------------------

        private Token createToken(AppUser user, TokenType type, String value, LocalDateTime expiration,
                        LocalDateTime useDate) {
                Token token = new Token();
                token.setValue(value);
                token.setTokenType(type);
                token.setUser(user);
                token.setCreationDateTime(LocalDateTime.now());
                token.setExpirationDateTime(expiration);
                token.setUseDate(useDate);
                token.setPendingData("");
                return tokenRepo.save(token);
        }

        @Test
        void verifyAccount_validToken_activatesUserInDatabase() throws Exception {
                AppUser user = createUser(uniqueEmail("a-activer"), AccountStatus.INACTIVE);
                String tokenValue = UUID.randomUUID().toString();
                createToken(user, TokenType.ENABLE_ACCOUNT, tokenValue, LocalDateTime.now().plusHours(1), null);

                mockMvc.perform(get("/account/verify").param("token", tokenValue))
                                .andExpect(status().isOk());

                AppUser refreshed = userRepo.findById(user.getId()).orElseThrow();
                org.assertj.core.api.Assertions.assertThat(refreshed.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void verifyAccount_invalidToken_returns400() throws Exception {
                mockMvc.perform(get("/account/verify").param("token", "token-qui-n-existe-pas"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void verifyAccount_expiredToken_returns400() throws Exception {
                AppUser user = createUser(uniqueEmail("expire"), AccountStatus.INACTIVE);
                String tokenValue = UUID.randomUUID().toString();
                createToken(user, TokenType.ENABLE_ACCOUNT, tokenValue, LocalDateTime.now().minusMinutes(1), null);

                mockMvc.perform(get("/account/verify").param("token", tokenValue))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void verifyAccount_alreadyUsedToken_returns400() throws Exception {
                AppUser user = createUser(uniqueEmail("deja-utilise"), AccountStatus.INACTIVE);
                String tokenValue = UUID.randomUUID().toString();
                createToken(user, TokenType.ENABLE_ACCOUNT, tokenValue, LocalDateTime.now().plusHours(1),
                                LocalDateTime.now().minusMinutes(5));

                mockMvc.perform(get("/account/verify").param("token", tokenValue))
                                .andExpect(status().isBadRequest());
        }

        // ---------------------------------------------------------------
        // US-202 : Login
        // ---------------------------------------------------------------

        @Test
        void login_correctCredentials_returns200WithToken() throws Exception {
                AppUser user = createUser(uniqueEmail("login-ok"), AccountStatus.ACTIVE);
                Map<String, String> payload = Map.of("email", user.getEmail(), "password", PASSWORD);

                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk())
                                .andExpect(result -> org.assertj.core.api.Assertions
                                                .assertThat(result.getResponse().getContentAsString())
                                                .contains("token"));
        }

        @Test
        void login_wrongPassword_isRejected() throws Exception {
                AppUser user = createUser(uniqueEmail("login-mauvais-mdp"), AccountStatus.ACTIVE);
                Map<String, String> payload = Map.of("email", user.getEmail(), "password", "mauvaisMotDePasse123");

                int status = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andReturn().getResponse().getStatus();

                org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(200);
        }

        @Test
        void login_inactiveAccount_isRejected() throws Exception {
                AppUser user = createUser(uniqueEmail("login-inactif"), AccountStatus.INACTIVE);
                Map<String, String> payload = Map.of("email", user.getEmail(), "password", PASSWORD);

                int status = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andReturn().getResponse().getStatus();

                org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(200);
        }

        // ---------------------------------------------------------------
        // Routes protégées
        // ---------------------------------------------------------------

        @Test
        void protectedRoute_withoutToken_isRejected() throws Exception {
                mockMvc.perform(get("/users"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void protectedRoute_withValidToken_isAccepted() throws Exception {
                AppUser admin = createUser(uniqueEmail("admin"), AccountStatus.ACTIVE);
                admin.setRole(Role.ADMINISTRATOR);
                userRepo.save(admin);

                Map<String, String> loginPayload = Map.of("email", admin.getEmail(), "password", PASSWORD);
                String loginResponse = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginPayload)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                String token = (String) objectMapper.readValue(loginResponse, Map.class).get("token");

                mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());
        }

        // ---------------------------------------------------------------
        // POST /account/password/change
        // ---------------------------------------------------------------

        private String loginAndGetToken(String email) throws Exception {
                Map<String, String> payload = Map.of("email", email, "password", PASSWORD);
                String response = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                return (String) objectMapper.readValue(response, Map.class).get("token");
        }

        @Test
        void changePassword_wrongCurrentPassword_returns400() throws Exception {
                AppUser user = createUser(uniqueEmail("change-mauvais-mdp"), AccountStatus.ACTIVE);
                String token = loginAndGetToken(user.getEmail());

                mockMvc.perform(post("/account/password/change")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("currentPassword", "mauvaisMotDePasse", "newPassword",
                                                                "nouveauMotDePasse123"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void changePassword_newPasswordTooShort_returns400() throws Exception {
                AppUser user = createUser(uniqueEmail("change-trop-court"), AccountStatus.ACTIVE);
                String token = loginAndGetToken(user.getEmail());

                mockMvc.perform(post("/account/password/change")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("currentPassword", PASSWORD, "newPassword", "court"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void changePassword_correctPassword_returns200AndSendsConfirmationEmail() throws Exception {
                AppUser user = createUser(uniqueEmail("change-ok"), AccountStatus.ACTIVE);
                String token = loginAndGetToken(user.getEmail());

                mockMvc.perform(post("/account/password/change")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("currentPassword", PASSWORD, "newPassword",
                                                                "nouveauMotDePasse123"))))
                                .andExpect(status().isOk());

                verify(emailService).sendPasswordChangeConfirmationEmail(
                                org.mockito.ArgumentMatchers.eq(user.getEmail()),
                                org.mockito.ArgumentMatchers.anyString());
                verify(emailService, org.mockito.Mockito.never()).sendPasswordResetEmail(
                                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        }

        @Test
        void resetPassword_newPasswordTooShort_returns400() throws Exception {
                mockMvc.perform(post("/account/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("token", "n-importe-quoi", "newPassword", "court"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPassword_unknownToken_returns400() throws Exception {
                mockMvc.perform(post("/account/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("token", "jeton-qui-n-existe-pas", "newPassword",
                                                                "nouveauMotDePasse123"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPassword_validToken_changesPasswordNowAndTokenCannotBeReused() throws Exception {
                AppUser user = createUser(uniqueEmail("reset-ok"), AccountStatus.ACTIVE);
                String tokenValue = UUID.randomUUID().toString();
                createToken(user, TokenType.CHANGE_PWD, tokenValue, LocalDateTime.now().plusHours(1), null);
                String newPassword = "nouveauMotDePasse123";
                String body = objectMapper.writeValueAsString(Map.of("token", tokenValue, "newPassword", newPassword));

                mockMvc.perform(post("/account/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("email", user.getEmail(), "password", newPassword))))
                                .andExpect(status().isOk());

                verify(emailService, org.mockito.Mockito.never()).sendPasswordChangeConfirmationEmail(
                                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

                mockMvc.perform(post("/account/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest());
        }

        // ---------------------------------------------------------------
        // Comptes suspendus
        // ---------------------------------------------------------------

        @Test
        void login_suspendedAccountWithCorrectPassword_returns403WithExplanation() throws Exception {
                AppUser user = createUser(uniqueEmail("login-suspendu"), AccountStatus.SUSPENDED);

                String body = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("email", user.getEmail(), "password", PASSWORD))))
                                .andExpect(status().isForbidden())
                                .andReturn().getResponse().getContentAsString();

                org.assertj.core.api.Assertions.assertThat(body).contains("suspendu");
        }

        @Test
        void login_suspendedAccountWithWrongPassword_doesNotRevealTheStatus() throws Exception {
                AppUser user = createUser(uniqueEmail("login-suspendu-faux-mdp"), AccountStatus.SUSPENDED);

                String body = mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("email", user.getEmail(), "password", "mauvaisMotDePasse123"))))
                                .andExpect(status().isUnauthorized())
                                .andReturn().getResponse().getContentAsString();

                org.assertj.core.api.Assertions.assertThat(body).doesNotContain("suspendu");
        }

        @Test
        void login_suspensionEnded_reactivatesTheAccountAndReturnsAToken() throws Exception {
                AppUser user = createUser(uniqueEmail("login-fin-suspension"), AccountStatus.SUSPENDED);
                user.setSuspensionEndDate(LocalDateTime.now().minusMinutes(1));
                userRepo.save(user);

                mockMvc.perform(post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                Map.of("email", user.getEmail(), "password", PASSWORD))))
                                .andExpect(status().isOk());

                AppUser refreshed = userRepo.findById(user.getId()).orElseThrow();
                org.assertj.core.api.Assertions.assertThat(refreshed.getStatus()).isEqualTo(AccountStatus.ACTIVE);
                org.assertj.core.api.Assertions.assertThat(refreshed.getSuspensionEndDate()).isNull();
        }

        @Test
        void protectedRoute_tokenOfAnAccountSuspendedAfterLogin_isRejected() throws Exception {
                AppUser admin = createUser(uniqueEmail("admin-suspendu"), AccountStatus.ACTIVE);
                admin.setRole(Role.ADMINISTRATOR);
                userRepo.save(admin);
                String token = loginAndGetToken(admin.getEmail());

                mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());

                admin.setStatus(AccountStatus.SUSPENDED);
                userRepo.save(admin);

                mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized());
        }
}
