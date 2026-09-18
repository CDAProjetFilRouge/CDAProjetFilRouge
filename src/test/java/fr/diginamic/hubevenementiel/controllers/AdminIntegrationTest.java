package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTest {

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

    // ---------------------------------------------------------------
    // EP5 : GET /users, POST /users/admin
    // ---------------------------------------------------------------

    @Test
    void getUsers_asAdmin_returns200() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getUsers_asMember_returns403() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createByAdmin_asAdmin_returns201WithRequestedRole() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());

        Map<String, Object> payload = Map.of(
                "firstName", "Nouveau",
                "lastName", "Organisateur",
                "email", uniqueEmail("cree-par-admin"),
                "phone", "0600000000",
                "role", "ORGANIZER"
        );

        mockMvc.perform(post("/users/admin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    void createByAdmin_asNonAdmin_returns403() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        String token = loginAndGetToken(organizer.getEmail());

        Map<String, Object> payload = Map.of(
                "firstName", "Nouveau",
                "lastName", "Admin",
                "email", uniqueEmail("tentative"),
                "phone", "0600000000",
                "role", "ADMINISTRATOR"
        );

        mockMvc.perform(post("/users/admin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Email d'information lors d'une modification hors mot de passe
    // ---------------------------------------------------------------

    @Test
    void updateOwnAccount_sendsInfoEmailWithoutPasswordEmail() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        String token = loginAndGetToken(member.getEmail());
        String newEmail = uniqueEmail("member-modifie");

        Map<String, Object> payload = Map.of(
                "firstName", "Modifie",
                "lastName", member.getLastName(),
                "email", newEmail,
                "phone", member.getPhone()
        );

        mockMvc.perform(put("/users/" + member.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(emailService).sendAccountInfoUpdatedEmail(newEmail);
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
        verify(emailService, never()).sendPasswordChangeConfirmationEmail(any(), any());
    }

    @Test
    void updateByAdmin_sendsInfoEmailWithoutPasswordEmail() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());
        AppUser target = createUser("cible", Role.MEMBER);
        String newEmail = uniqueEmail("cible-modifiee");

        Map<String, Object> payload = Map.of(
                "firstName", target.getFirstName(),
                "lastName", target.getLastName(),
                "email", newEmail,
                "phone", target.getPhone(),
                "role", "ORGANIZER"
        );

        mockMvc.perform(put("/users/" + target.getId() + "/admin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(emailService).sendAccountInfoUpdatedEmail(newEmail);
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
        verify(emailService, never()).sendPasswordChangeConfirmationEmail(any(), any());
    }

    // ---------------------------------------------------------------
    // EP5 : CRUD clubs
    // ---------------------------------------------------------------

    private String validClubPayload(String name) throws Exception {
        AddressRequestDto address = new AddressRequestDto();
        address.setStreet1("1 rue du Club");
        address.setPostalCode("75001");
        address.setCity("Paris");
        address.setCountry("France");

        Map<String, Object> body = Map.of(
                "name", name,
                "category", Category.SPORT,
                "email", "club-" + System.nanoTime() + "@example.com",
                "phone", "0600000000",
                "address", address
        );
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void clubCrud_createListUpdateDelete_happyPath() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());
        String name = "Club de test-" + System.nanoTime();

        String createResponse = mockMvc.perform(post("/clubs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubPayload(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long clubId = ((Number) objectMapper.readValue(createResponse, Map.class).get("id")).longValue();

        mockMvc.perform(get("/clubs?size=200").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .contains(name));

        mockMvc.perform(put("/clubs/" + clubId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubPayload(name + "-modifie")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/clubs/" + clubId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void createClub_duplicateName_returns409() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());
        String name = "Club duplique-" + System.nanoTime();

        mockMvc.perform(post("/clubs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubPayload(name)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clubs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubPayload(name)))
                .andExpect(status().isConflict());
    }

    @Test
    void createClub_validationError_returns400() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = loginAndGetToken(admin.getEmail());

        String payload = """
                {"name":"Club sans categorie"}
                """;

        mockMvc.perform(post("/clubs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClub_asNonAdmin_returns403() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        String token = loginAndGetToken(organizer.getEmail());

        mockMvc.perform(post("/clubs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validClubPayload("Club interdit-" + System.nanoTime())))
                .andExpect(status().isForbidden());
    }
}
