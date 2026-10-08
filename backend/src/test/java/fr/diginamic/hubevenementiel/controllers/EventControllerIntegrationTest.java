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
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    @Autowired
    private fr.diginamic.hubevenementiel.repositories.ClubRepo clubRepo;

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

        mockMvc.perform(get("/events/filter")
                        .param("keyword", title)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/events/filter")
                        .param("keyword", title)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getEvents_sizeAboveLimit_returns400() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());

        mockMvc.perform(get("/events?size=100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
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
    void createEvent_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload("Sans token-" + System.nanoTime())))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // GET /events/filter : filtres, dates inclusives, places restantes
    // ---------------------------------------------------------------

    @Test
    void filterEvents_byCity_returnsOnlyMatchingEvents() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());
        String city = "Ville-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String inCity = "Dans la ville-" + System.nanoTime();
        String elsewhere = "Ailleurs-" + System.nanoTime();
        createPublishedEvent(token, inCity, city, start, start.plusHours(2), 50);
        createPublishedEvent(token, elsewhere, "Paris", start, start.plusHours(2), 50);

        mockMvc.perform(get("/events/filter")
                        .param("city", city)
                        .param("status", "PUBLISHED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(inCity));
    }

    @Test
    void filterEvents_endDateIsInclusive_startDateExcludesEarlierEvents() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());
        String city = "Ville-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        createPublishedEvent(token, "Evenement date-" + System.nanoTime(), city, start, start.plusHours(2), 50);
        String day = start.toLocalDate().toString();
        String nextDay = start.toLocalDate().plusDays(1).toString();

        mockMvc.perform(get("/events/filter")
                        .param("city", city).param("status", "PUBLISHED")
                        .param("startDate", day).param("endDate", day)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/events/filter")
                        .param("city", city).param("status", "PUBLISHED")
                        .param("startDate", nextDay)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filterEvents_withoutInscriptions_remainingSpotsEqualsCapacity() throws Exception {
        String token = loginAndGetToken(organizer.getEmail());
        String city = "Ville-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        createPublishedEvent(token, "Evenement places-" + System.nanoTime(), city, start, start.plusHours(2), 42);

        mockMvc.perform(get("/events/filter")
                        .param("city", city).param("status", "PUBLISHED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maxCapacity").value(42))
                .andExpect(jsonPath("$.content[0].remainingSpots").value(42));
    }

    @Test
    void filterEvents_listOfEventsWithDifferentOrganizers_doesNotQueryPerEvent() throws Exception {
        String city = "Ville-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String viewerToken = loginAndGetToken(organizer.getEmail());
        for (int i = 0; i < 5; i++) {
            AppUser eventOrganizer = createActiveUser("n1-" + i + "-" + System.nanoTime() + "@example.com", Role.ORGANIZER);
            String token = loginAndGetToken(eventOrganizer.getEmail());
            createPublishedEvent(token, "Evenement N+1 " + i + "-" + System.nanoTime(), city, start, start.plusHours(2), 50);
        }
        entityManager.flush();
        entityManager.clear();

        org.hibernate.stat.Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        mockMvc.perform(get("/events/filter")
                        .param("city", city).param("status", "PUBLISHED")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));

        org.assertj.core.api.Assertions.assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    @Test
    void filterEvents_showsOrganizerClubsInsteadOfOrganizerName() throws Exception {
        String clubName = "Club-" + System.nanoTime();
        fr.diginamic.hubevenementiel.entities.Club club = new fr.diginamic.hubevenementiel.entities.Club();
        club.setName(clubName);
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        club = clubRepo.save(club);
        organizer.getClubs().add(club);
        userRepo.save(organizer);

        String token = loginAndGetToken(organizer.getEmail());
        String city = "Ville-" + System.nanoTime();
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        createPublishedEvent(token, "Evenement club-" + System.nanoTime(), city, start, start.plusHours(2), 50);

        mockMvc.perform(get("/events/filter")
                        .param("city", city).param("status", "PUBLISHED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].organizerClubs[0]").value(clubName))
                .andExpect(jsonPath("$.content[0].organizerFirstName").doesNotExist())
                .andExpect(jsonPath("$.content[0].organizerLastName").doesNotExist());
    }

    @Test
    void filterEvents_byClubName_returnsOnlyEventsOfThatClubOrganizers() throws Exception {
        String clubName = "Club-" + System.nanoTime();
        fr.diginamic.hubevenementiel.entities.Club club = new fr.diginamic.hubevenementiel.entities.Club();
        club.setName(clubName);
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        club = clubRepo.save(club);
        organizer.getClubs().add(club);
        userRepo.save(organizer);

        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String inClub = "Evenement du club-" + System.nanoTime();
        createPublishedEvent(loginAndGetToken(organizer.getEmail()), inClub, "Paris", start, start.plusHours(2), 50);
        createPublishedEvent(loginAndGetToken(otherOrganizer.getEmail()), "Evenement hors club-" + System.nanoTime(),
                "Paris", start, start.plusHours(2), 50);

        mockMvc.perform(get("/events/filter")
                        .param("clubName", clubName.toUpperCase()).param("status", "PUBLISHED")
                        .header("Authorization", "Bearer " + loginAndGetToken(organizer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(inClub));
    }

    @Test
    void uploadedImages_areReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/uploads/fichier-inexistant.png"))
                .andExpect(status().isNotFound());
    }

    private void createPublishedEvent(String token, String title, String city, LocalDateTime start,
            LocalDateTime end, int capacity) throws Exception {
        AddressRequestDto address = new AddressRequestDto();
        address.setStreet1("12 rue de la Paix");
        address.setPostalCode("75002");
        address.setCity(city);
        address.setCountry("France");
        Map<String, Object> body = Map.of(
                "title", title,
                "description", "Un bel évènement de test.",
                "location", address,
                "category", Category.SPORT,
                "startDateTime", start.toString(),
                "endDateTime", end.toString(),
                "affiliatePrice", 10,
                "nonAffiliatePrice", 20,
                "maxCapacity", capacity
        );

        String response = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long eventId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        Map<String, Object> published = new java.util.HashMap<>(body);
        published.put("status", "PUBLISHED");
        mockMvc.perform(put("/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(published)))
                .andExpect(status().isOk());
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
