package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.RefreshToken;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.RefreshTokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.AppUserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class RefreshTokenIntegrationTest {

    private static final String PASSWORD = "motdepasse123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private RefreshTokenRepo refreshTokenRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AppUserService appUserService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("refresh-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.MEMBER);
        user.setStatus(AccountStatus.ACTIVE);
        user = userRepo.save(user);
    }

    private Map<?, ?> login() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD));
        String response = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        syncWithDatabase();
        return objectMapper.readValue(response, Map.class);
    }

    private int refresh(String refreshToken) throws Exception {
        int httpStatus = mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andReturn().getResponse().getStatus();
        syncWithDatabase();
        return httpStatus;
    }

    private Map<?, ?> refreshOk(String refreshToken) throws Exception {
        String response = mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        syncWithDatabase();
        return objectMapper.readValue(response, Map.class);
    }

    private void syncWithDatabase() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<RefreshToken> tokensOfUser() {
        return refreshTokenRepo.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .toList();
    }

    @Test
    void login_returnsAnAccessTokenAndARefreshToken() throws Exception {
        Map<?, ?> tokens = login();

        assertThat((String) tokens.get("token")).isNotBlank();
        assertThat((String) tokens.get("refreshToken")).isNotBlank();
    }

    @Test
    void login_storesOnlyTheHashOfTheRefreshToken() throws Exception {
        String refreshToken = (String) login().get("refreshToken");

        List<RefreshToken> stored = tokensOfUser();

        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getTokenHash()).isNotEqualTo(refreshToken).hasSize(64);
    }

    @Test
    void refresh_validToken_returnsNewTokensAndRotates() throws Exception {
        String first = (String) login().get("refreshToken");

        Map<?, ?> renewed = refreshOk(first);

        assertThat((String) renewed.get("token")).isNotBlank();
        assertThat((String) renewed.get("refreshToken")).isNotBlank().isNotEqualTo(first);
        assertThat(tokensOfUser()).hasSize(2);
        assertThat(tokensOfUser().stream().map(RefreshToken::getFamilyId).distinct()).hasSize(1);
    }

    @Test
    void refresh_unknownToken_returns401() throws Exception {
        assertThat(refresh("ce-jeton-nexiste-pas")).isEqualTo(401);
    }

    @Test
    void refresh_sameTokenAgainWithinGraceWindow_isAccepted() throws Exception {
        String first = (String) login().get("refreshToken");
        refreshOk(first);

        assertThat(refresh(first)).isEqualTo(200);
    }

    @Test
    void refresh_reusedTokenAfterGraceWindow_returns401AndRevokesTheWholeFamily() throws Exception {
        String first = (String) login().get("refreshToken");
        String second = (String) refreshOk(first).get("refreshToken");
        RefreshToken used = tokensOfUser().stream().filter(token -> token.getUsedAt() != null).findFirst().orElseThrow();
        used.setUsedAt(LocalDateTime.now().minusMinutes(1));
        syncWithDatabase();

        assertThat(refresh(first)).isEqualTo(401);

        assertThat(refresh(second)).isEqualTo(401);
        assertThat(tokensOfUser()).allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void logout_revokesTheSession() throws Exception {
        String refreshToken = (String) login().get("refreshToken");

        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());
        syncWithDatabase();

        assertThat(refresh(refreshToken)).isEqualTo(401);
    }

    @Test
    void logout_unknownToken_stillReturns204() throws Exception {
        mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "inconnu"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void refresh_expiredToken_returns401() throws Exception {
        String refreshToken = (String) login().get("refreshToken");
        entityManager.createQuery("update RefreshToken t set t.expiresAt = :value where t.user.id = :userId")
                .setParameter("value", LocalDateTime.now().minusMinutes(1))
                .setParameter("userId", user.getId())
                .executeUpdate();
        syncWithDatabase();

        assertThat(refresh(refreshToken)).isEqualTo(401);
    }

    @Test
    void refresh_familyOlderThanAbsoluteLimit_returns401() throws Exception {
        String refreshToken = (String) login().get("refreshToken");
        RefreshToken stored = tokensOfUser().get(0);
        stored.setUsedAt(null);
        setFamilyCreatedAt(stored, LocalDateTime.now().minusDays(31));
        syncWithDatabase();

        assertThat(refresh(refreshToken)).isEqualTo(401);
    }

    @Test
    void refresh_suspendedUser_returns401AndRevokesTheFamily() throws Exception {
        String refreshToken = (String) login().get("refreshToken");
        AppUser suspended = userRepo.findById(user.getId()).orElseThrow();
        suspended.setStatus(AccountStatus.SUSPENDED);
        syncWithDatabase();

        assertThat(refresh(refreshToken)).isEqualTo(401);
        assertThat(tokensOfUser()).allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void refresh_newTokenNeverOutlivesTheAbsoluteLimit() throws Exception {
        String refreshToken = (String) login().get("refreshToken");
        RefreshToken stored = tokensOfUser().get(0);
        LocalDateTime familyStart = LocalDateTime.now().minusDays(29);
        setFamilyCreatedAt(stored, familyStart);
        syncWithDatabase();

        refreshOk(refreshToken);

        LocalDateTime newest = tokensOfUser().stream().filter(token -> token.getUsedAt() == null)
                .map(RefreshToken::getExpiresAt).findFirst().orElseThrow();
        assertThat(newest).isBeforeOrEqualTo(familyStart.plusDays(30));
    }

    @Test
    void deleteAccount_userWithSessions_removesTheirRefreshTokens() throws Exception {
        login();
        assertThat(tokensOfUser()).hasSize(1);

        appUserService.deleteAccount(user.getId(), false);
        syncWithDatabase();

        assertThat(userRepo.findById(user.getId())).isEmpty();
        assertThat(refreshTokenRepo.findAll()).noneMatch(token -> token.getUser().getId().equals(user.getId()));
    }

    private static final String BASE_URL = "http://localhost";

    private KeyPair newKey() {
        return Jwts.SIG.ES256.keyPair().build();
    }

    private String thumbprintOf(KeyPair key) {
        return Jwks.builder().key(key.getPublic()).build().thumbprint().toString();
    }

    private String dpopProof(KeyPair signer, String path) {
        return Jwts.builder()
                .header().type("dpop+jwt").jwk(Jwks.builder().key(signer.getPublic()).build()).and()
                .id(UUID.randomUUID().toString())
                .claim("htm", "POST")
                .claim("htu", BASE_URL + path)
                .claim("iat", Instant.now().getEpochSecond())
                .signWith(signer.getPrivate(), Jwts.SIG.ES256)
                .compact();
    }

    private int loginStatus(String dpopHeader) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD));
        var request = post("/login").contentType(MediaType.APPLICATION_JSON).content(body);
        if (dpopHeader != null) {
            request = request.header("DPoP", dpopHeader);
        }
        int httpStatus = mockMvc.perform(request).andReturn().getResponse().getStatus();
        syncWithDatabase();
        return httpStatus;
    }

    private String loginWithKey(KeyPair key) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD));
        String response = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("DPoP", dpopProof(key, "/login")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        syncWithDatabase();
        return (String) objectMapper.readValue(response, Map.class).get("refreshToken");
    }

    private int refreshStatus(String refreshToken, KeyPair signer) throws Exception {
        var request = post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
        if (signer != null) {
            request = request.header("DPoP", dpopProof(signer, "/auth/refresh"));
        }
        int httpStatus = mockMvc.perform(request).andReturn().getResponse().getStatus();
        syncWithDatabase();
        return httpStatus;
    }

    @Test
    void login_withProof_bindsTheSessionToTheKey() throws Exception {
        KeyPair key = newKey();

        loginWithKey(key);

        assertThat(tokensOfUser()).hasSize(1);
        assertThat(tokensOfUser().get(0).getKeyThumbprint()).isEqualTo(thumbprintOf(key));
    }

    @Test
    void login_withoutProof_leavesTheSessionUnbound() throws Exception {
        login();

        assertThat(tokensOfUser().get(0).getKeyThumbprint()).isNull();
    }

    @Test
    void login_withInvalidProof_returns401AndOpensNoSession() throws Exception {
        assertThat(loginStatus("ceci-nest-pas-une-preuve")).isEqualTo(401);

        assertThat(tokensOfUser()).isEmpty();
    }

    @Test
    void refresh_withTheSameKey_isAcceptedAndKeepsTheBinding() throws Exception {
        KeyPair key = newKey();
        String first = loginWithKey(key);

        assertThat(refreshStatus(first, key)).isEqualTo(200);

        assertThat(tokensOfUser()).hasSize(2);
        assertThat(tokensOfUser()).allMatch(token -> thumbprintOf(key).equals(token.getKeyThumbprint()));
    }

    @Test
    void refresh_withAnotherKey_returns401AndRevokesTheFamily() throws Exception {
        String first = loginWithKey(newKey());

        assertThat(refreshStatus(first, newKey())).isEqualTo(401);

        assertThat(tokensOfUser()).allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void refresh_boundSessionWithoutProof_returns401AndRevokesTheFamily() throws Exception {
        String first = loginWithKey(newKey());

        assertThat(refreshStatus(first, null)).isEqualTo(401);

        assertThat(tokensOfUser()).allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void refresh_unboundSessionWithAProof_isAcceptedButStaysUnbound() throws Exception {
        String first = (String) login().get("refreshToken");

        assertThat(refreshStatus(first, newKey())).isEqualTo(200);

        assertThat(tokensOfUser()).hasSize(2);
        assertThat(tokensOfUser()).allMatch(token -> token.getKeyThumbprint() == null);
    }

    private void setFamilyCreatedAt(RefreshToken token, LocalDateTime value) {
        entityManager.createQuery("update RefreshToken t set t.familyCreatedAt = :value where t.familyId = :family")
                .setParameter("value", value)
                .setParameter("family", token.getFamilyId())
                .executeUpdate();
    }
}
