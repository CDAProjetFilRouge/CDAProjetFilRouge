package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test d'intégration bout-en-bout : vraie base (MariaDB, pointée via les variables
// d'env habituelles ou le service CI), vrai contexte Spring, vrai flux JWT (login
// reel via /login). @Transactional fait rollback de tout a la fin de chaque test,
// donc aucune pollution durable de la base utilisee.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "motdepasse123456";

    private AppUser organizer;
    private AppUser otherOrganizer;

    @BeforeEach
    void setUp() {
        organizer = createActiveUser("organizer-" + System.nanoTime() + "@example.com", Role.ORGANIZER);
        otherOrganizer = createActiveUser("other-organizer-" + System.nanoTime() + "@example.com", Role.ORGANIZER);
    }

    private AppUser createActiveUser(String email, Role role) {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        return userRepo.save(user);
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequestDto login = new LoginRequestDto();
        login.setEmail(email);
        login.setPassword(PASSWORD);

        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return (String) objectMapper.readValue(response, Map.class).get("token");
    }

    private String validEventPayload(String title) throws Exception {
        AddressRequestDto address = new AddressRequestDto();
        address.setStreet1("12 rue de la Paix");
        address.setPostalCode("75002");
        address.setCity("Paris");
        address.setCountry("France");

        Map<String, Object> body = Map.of(
                "title", title,
                "description", "Un bel évènement de test.",
                "location", address,
                "category", Category.SPORT,
                "startDateTime", LocalDateTime.now().plusDays(10).toString(),
                "endDateTime", LocalDateTime.now().plusDays(10).plusHours(2).toString(),
                "affiliatePrice", 10,
                "nonAffiliatePrice", 20,
                "maxCapacity", 50
        );

        return objectMapper.writeValueAsString(body);
    }

    // ---------------------------------------------------------------
    // US-400/US-401 : création et publication
    // ---------------------------------------------------------------

    @Test
    void createEvent_validPayload_returns201WithDraftStatus() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload("Marathon IT-" + System.nanoTime())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.organizer.id").value(organizer.getId()));
    }

    @Test
    void createEvent_missingCategory_returns400() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());
        String payload = """
                {"title":"Event sans categorie","description":"desc","maxCapacity":10}
                """;

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_malformedJson_returns400() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ceci n'est pas du json valide"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishEvent_setsStatusPublishedInDatabase() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());
        Long eventId = createDraftEventAndReturnId(token, "Event a publier-" + System.nanoTime());

        AddressRequestDto address = new AddressRequestDto();
        address.setStreet1("12 rue de la Paix");
        address.setPostalCode("75002");
        address.setCity("Paris");
        address.setCountry("France");
        Map<String, Object> updatePayload = Map.of(
                "title", "Event publie-" + System.nanoTime(),
                "description", "desc",
                "location", address,
                "category", Category.SPORT,
                "startDateTime", LocalDateTime.now().plusDays(10).toString(),
                "endDateTime", LocalDateTime.now().plusDays(10).plusHours(2).toString(),
                "affiliatePrice", 10,
                "nonAffiliatePrice", 20,
                "maxCapacity", 50,
                "status", "PUBLISHED"
        );

        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    // ---------------------------------------------------------------
    // RG14 : les DRAFT n'apparaissent pas dans le listing pour un tiers
    // ---------------------------------------------------------------

    @Test
    void getEvents_draftEvent_excludedForNonOwner() throws Exception {
        String ownerToken = loginAndGetToken(organizer.getEmail());
        String title = "Draft prive-" + System.nanoTime();
        createDraftEventAndReturnId(ownerToken, title);

        String otherToken = loginAndGetToken(otherOrganizer.getEmail());

        mockMvc.perform(get("/events?size=100")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(title);
                });
    }

    // ---------------------------------------------------------------
    // RG15 : seul le propriétaire peut modifier/supprimer
    // ---------------------------------------------------------------

    @Test
    void updateEvent_byNonOwner_returns403() throws Exception {
        String ownerToken = loginAndGetToken(organizer.getEmail());
        Long eventId = createDraftEventAndReturnId(ownerToken, "Event proprietaire-" + System.nanoTime());

        String otherToken = loginAndGetToken(otherOrganizer.getEmail());

        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload("Tentative de hack-" + System.nanoTime())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_byNonOwner_returns403() throws Exception {
        String ownerToken = loginAndGetToken(organizer.getEmail());
        Long eventId = createDraftEventAndReturnId(ownerToken, "Event a supprimer-" + System.nanoTime());

        String otherToken = loginAndGetToken(otherOrganizer.getEmail());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Routes protégées : sans token
    // ---------------------------------------------------------------

    @Test
    void createEvent_withoutToken_returns403() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload("Sans token-" + System.nanoTime())))
                .andExpect(status().isForbidden());
    }

    private Long createDraftEventAndReturnId(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();
    }
}
