package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class RgpdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String PASSWORD = "motdepasse123456";

    private String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }

    private AppUser createUser(String prefix, Role role) {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(uniqueEmail(prefix));
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setClubs(java.util.List.of());
        return userRepo.save(user);
    }

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
    void requestAnonymization_returns201ForSelf() throws Exception {
        AppUser requester = createUser("requester", Role.MEMBER);
        String token = loginAndGetToken(requester.getEmail());

        mockMvc.perform(post("/anonymization-demands").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requester.id").value(requester.getId()))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"));
    }

    @Test
    void validateAnonymization_asAdmin_reallyAnonymizesPersonalData() throws Exception {
        AppUser requester = createUser("a-anonymiser", Role.MEMBER);
        String originalEmail = requester.getEmail();
        String requesterToken = loginAndGetToken(requester.getEmail());

        String demandResponse = mockMvc.perform(post("/anonymization-demands")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long demandId = ((Number) objectMapper.readValue(demandResponse, Map.class).get("id")).longValue();

        AppUser admin = createUser("admin-rgpd", Role.ADMINISTRATOR);
        String adminToken = loginAndGetToken(admin.getEmail());

        mockMvc.perform(put("/anonymization-demands/" + demandId + "/validate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("VALIDATE"))
                .andExpect(jsonPath("$.admin.id").value(admin.getId()));

        AppUser refreshed = userRepo.findById(requester.getId()).orElseThrow();
        assertThat(refreshed.getEmail()).isNotEqualTo(originalEmail);
        assertThat(refreshed.getEmail()).contains("@anonymise.local");
        assertThat(refreshed.getFirstName()).startsWith("ANONYME-");
        assertThat(refreshed.getLastName()).startsWith("ANONYME-");
        assertThat(refreshed.getStatus()).isEqualTo(AccountStatus.ANONYMIZE);
        assertThat(refreshed.getAddress()).isNull();
    }

    @Test
    void validateAnonymization_asNonAdmin_returns403() throws Exception {
        AppUser requester = createUser("a-anonymiser2", Role.MEMBER);
        String requesterToken = loginAndGetToken(requester.getEmail());

        String demandResponse = mockMvc.perform(post("/anonymization-demands")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long demandId = ((Number) objectMapper.readValue(demandResponse, Map.class).get("id")).longValue();

        mockMvc.perform(put("/anonymization-demands/" + demandId + "/validate")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateAnonymization_adminIdComesFromAuthenticatedPrincipal() throws Exception {
        AppUser requester = createUser("a-anonymiser3", Role.MEMBER);
        String requesterToken = loginAndGetToken(requester.getEmail());

        String demandResponse = mockMvc.perform(post("/anonymization-demands")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long demandId = ((Number) objectMapper.readValue(demandResponse, Map.class).get("id")).longValue();

        AppUser adminA = createUser("admin-a", Role.ADMINISTRATOR);
        String adminAToken = loginAndGetToken(adminA.getEmail());

        mockMvc.perform(put("/anonymization-demands/" + demandId + "/validate")
                        .header("Authorization", "Bearer " + adminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin.id").value(adminA.getId()));
    }
}
