package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class RoleAccessIntegrationTest {

    private static final String PASSWORD = "motdepasse123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ClubRepo clubRepo;
    @Autowired
    private EventRepo eventRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppUser createUser(String prefix, Role role) {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        return userRepo.save(user);
    }

    private String login(AppUser user) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Club createClub(AppUser owner, AppUser member) {
        Club club = new Club();
        club.setName("Club " + System.nanoTime());
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        club.setOwner(owner);
        club.getAppUsers().add(member);
        return clubRepo.save(club);
    }

    private Event createEvent(AppUser organizer, Club club, EventStatus status, String title) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Description");
        event.setCategory(Category.SPORT);
        event.setStartDateTime(LocalDateTime.now().plusDays(10));
        event.setEndDateTime(LocalDateTime.now().plusDays(10).plusHours(2));
        event.setAffiliatePrice(BigDecimal.TEN);
        event.setNonAffiliatePrice(BigDecimal.TEN);
        event.setMaxCapacity(10);
        event.setStatus(status);
        event.setOrganizer(organizer);
        event.setClub(club);
        return eventRepo.save(event);
    }

    private MockHttpServletRequestBuilder with(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private int statusOf(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(with(request, token)).andReturn().getResponse().getStatus();
    }

    // ---------------------------------------------------------------
    // Rôle relu en base à chaque requête
    // ---------------------------------------------------------------

    @Test
    void demotedAdministratorLosesHisRightsImmediatelyEvenWithAValidToken() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = login(admin);
        assertThat(statusOf(get("/users"), token)).isEqualTo(200);

        admin.setRole(Role.MEMBER);
        userRepo.saveAndFlush(admin);

        assertThat(statusOf(get("/users"), token)).isEqualTo(403);
    }

    @Test
    void promotedUserGetsHisNewRightsWithoutLoggingInAgain() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        String token = login(member);
        assertThat(statusOf(get("/users"), token)).isEqualTo(403);

        member.setRole(Role.ADMINISTRATOR);
        userRepo.saveAndFlush(member);

        assertThat(statusOf(get("/users"), token)).isEqualTo(200);
    }

    // ---------------------------------------------------------------
    // Matrice des routes réservées aux administrateurs
    // ---------------------------------------------------------------

    private List<MockHttpServletRequestBuilder> adminRoutes() {
        UUID id = UUID.randomUUID();
        String json = "{}";
        return List.of(
                get("/users"),
                get("/users/" + id),
                post("/users/admin").contentType(MediaType.APPLICATION_JSON).content(json),
                put("/users/" + id + "/suspend").contentType(MediaType.APPLICATION_JSON).content(json),
                put("/users/" + id + "/reactivate"),
                put("/users/" + id + "/admin").contentType(MediaType.APPLICATION_JSON).content(json),
                delete("/users/" + id),
                get("/clubs/admin"),
                post("/clubs").contentType(MediaType.APPLICATION_JSON).content(json),
                put("/clubs/" + id).contentType(MediaType.APPLICATION_JSON).content(json),
                delete("/clubs/" + id),
                get("/anonymization-demands"),
                get("/anonymization-demands/count"),
                put("/anonymization-demands/" + id + "/validate"),
                post("/legal-documents").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void adminRoutesRefuseAnonymousMemberAndOrganizer() throws Exception {
        String memberToken = login(createUser("member", Role.MEMBER));
        String organizerToken = login(createUser("orga", Role.ORGANIZER));

        for (MockHttpServletRequestBuilder route : adminRoutes()) {
            String description = route.buildRequest(new org.springframework.mock.web.MockServletContext()).getMethod()
                    + " " + route.buildRequest(new org.springframework.mock.web.MockServletContext()).getRequestURI();

            assertThat(statusOf(route, null)).as("anonyme : %s", description).isEqualTo(401);
            assertThat(statusOf(route, memberToken)).as("membre : %s", description).isEqualTo(403);
            assertThat(statusOf(route, organizerToken)).as("organisateur : %s", description).isEqualTo(403);
        }
    }

    @Test
    void adminRoutesAreReachableByAnAdministrator() throws Exception {
        String adminToken = login(createUser("admin", Role.ADMINISTRATOR));

        for (MockHttpServletRequestBuilder route : adminRoutes()) {
            String description = route.buildRequest(new org.springframework.mock.web.MockServletContext()).getMethod()
                    + " " + route.buildRequest(new org.springframework.mock.web.MockServletContext()).getRequestURI();
            int httpStatus = statusOf(route, adminToken);

            assertThat(httpStatus).as("administrateur : %s", description).isNotIn(401, 403);
        }
    }

    @Test
    void organizerRoutesRefuseMembersAndAnonymous() throws Exception {
        String memberToken = login(createUser("member", Role.MEMBER));
        UUID id = UUID.randomUUID();
        List<MockHttpServletRequestBuilder> routes = List.of(
                get("/events/mine"),
                post("/events").contentType(MediaType.APPLICATION_JSON).content("{}"),
                put("/events/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/events/" + id),
                delete("/inscriptions/" + id + "/organizer"));

        for (MockHttpServletRequestBuilder route : routes) {
            assertThat(statusOf(route, null)).isEqualTo(401);
            assertThat(statusOf(route, memberToken)).isEqualTo(403);
        }
    }

    // ---------------------------------------------------------------
    // Brouillons : visibles de leur organisateur et des administrateurs seulement
    // ---------------------------------------------------------------

    @Test
    void draftPdfIsHiddenFromAnonymousAndOtherMembers() throws Exception {
        AppUser organizer = createUser("orga", Role.ORGANIZER);
        AppUser stranger = createUser("member", Role.MEMBER);
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        Club club = createClub(organizer, stranger);
        Event draft = createEvent(organizer, club, EventStatus.DRAFT, "Brouillon " + System.nanoTime());
        String url = "/PDF/event/" + draft.getId();

        assertThat(statusOf(get(url), null)).isEqualTo(404);
        assertThat(statusOf(get(url), login(stranger))).isEqualTo(404);
        assertThat(statusOf(get(url), login(organizer))).isEqualTo(200);
        assertThat(statusOf(get(url), login(admin))).isEqualTo(200);
    }

    @Test
    void publishedPdfStaysPublic() throws Exception {
        AppUser organizer = createUser("orga", Role.ORGANIZER);
        Club club = createClub(organizer, organizer);
        Event published = createEvent(organizer, club, EventStatus.PUBLISHED, "Publié " + System.nanoTime());

        assertThat(statusOf(get("/PDF/event/" + published.getId()), null)).isEqualTo(200);
    }

    @Test
    void searchByTitleDoesNotRevealSomeoneElsesDraft() throws Exception {
        AppUser organizer = createUser("orga", Role.ORGANIZER);
        AppUser stranger = createUser("member", Role.MEMBER);
        Club club = createClub(organizer, stranger);
        String title = "Brouillon-secret-" + System.nanoTime();
        createEvent(organizer, club, EventStatus.DRAFT, title);

        assertThat(statusOf(get("/events/search").param("name", title), login(stranger))).isEqualTo(404);
        assertThat(statusOf(get("/events/search").param("name", title), login(organizer))).isEqualTo(200);
    }

    // ---------------------------------------------------------------
    // Détail d'un club : la liste des membres (noms, e-mails) n'est pas publique
    // ---------------------------------------------------------------

    private JsonNode clubDetail(UUID clubId, String token) throws Exception {
        MvcResult result = mockMvc.perform(with(get("/clubs/" + clubId), token)).andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void clubMembersAreOnlyListedForTheOwnerAndAdministrators() throws Exception {
        AppUser owner = createUser("orga", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        AppUser stranger = createUser("stranger", Role.MEMBER);
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        Club club = createClub(owner, member);

        assertThat(clubDetail(club.getId(), login(stranger)).get("appUsers")).isEmpty();
        assertThat(clubDetail(club.getId(), login(owner)).get("appUsers")).hasSize(1);
        assertThat(clubDetail(club.getId(), login(admin)).get("appUsers")).hasSize(1);
    }

    @Test
    void associatingAUserToAClubRevealsNothingToANonOwner() throws Exception {
        AppUser owner = createUser("orga", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        AppUser otherOrganizer = createUser("orga2", Role.ORGANIZER);
        Club club = createClub(owner, member);

        MvcResult result = mockMvc.perform(with(
                post("/clubs/associateUser/" + member.getId() + "/club/" + club.getId()), login(otherOrganizer)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getContentAsString()).contains("propriétaire");
    }
}
