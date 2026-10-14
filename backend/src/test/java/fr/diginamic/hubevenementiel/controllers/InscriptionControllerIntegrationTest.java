package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class InscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EventRepo eventRepo;
    @Autowired
    private ClubRepo clubRepo;
    @Autowired
    private InscriptionRepo inscriptionRepo;
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

    private Club createClub() {
        Club club = new Club();
        club.setName("Club-" + System.nanoTime());
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        return clubRepo.save(club);
    }

    private Event createEvent(AppUser organizer, int maxCapacity, LocalDateTime start, EventStatus status) {
        Event event = new Event();
        event.setTitle("Event-" + System.nanoTime());
        event.setDescription("desc");
        event.setCategory(Category.SPORT);
        event.setStartDateTime(start);
        event.setEndDateTime(start.plusHours(2));
        event.setMaxCapacity(maxCapacity);
        event.setAffiliatePrice(BigDecimal.TEN);
        event.setNonAffiliatePrice(BigDecimal.valueOf(20));
        event.setStatus(status);
        event.setOrganizer(organizer);
        event.setClub(createClub());
        return eventRepo.save(event);
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
    // US-300/US-301 : inscription à un évènement
    // ---------------------------------------------------------------

    @Test
    void register_spotAvailable_returns201Confirmed() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // ---------------------------------------------------------------
    // contrôles d'identité (audit sécurité)
    // ---------------------------------------------------------------

    @Test
    void register_forAnotherUser_returns403AndCreatesNothing() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser attacker = createUser("attacker", Role.MEMBER);
        AppUser victim = createUser("victim", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + loginAndGetToken(attacker.getEmail()))
                        .param("userId", String.valueOf(victim.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/inscriptions/user/" + victim.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(victim.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userInscriptions_ownerAndAdministratorCanRead_otherMemberCannot() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        AppUser other = createUser("other", Role.MEMBER);
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);

        mockMvc.perform(get("/inscriptions/user/" + member.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(member.getEmail())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/inscriptions/user/" + member.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(admin.getEmail())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/inscriptions/user/" + member.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(other.getEmail())))
                .andExpect(status().isForbidden());
    }

    // Le calendrier affiche le club de l'évènement dans la bulle : il doit figurer dans les inscriptions de l'utilisateur.
    @Test
    void userInscriptions_carryTheClubNameOfTheEvent() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        String token = loginAndGetToken(member.getEmail());
        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/inscriptions/user/" + member.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event.clubName").value(event.getClub().getName()));
    }

    @Test
    void userInscriptions_invalidPaging_returns400() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(get("/inscriptions/user/" + member.getId()).param("size", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/inscriptions/user/" + member.getId()).param("page", "-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventInscriptions_onlyOwnerAndAdministratorCanRead() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        AppUser otherOrganizer = createUser("other-organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        Event event = createEvent(owner, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        mockMvc.perform(get("/inscriptions/event/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(owner.getEmail())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/inscriptions/event/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(admin.getEmail())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/inscriptions/event/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(otherOrganizer.getEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/inscriptions/event/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(member.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void eventInscriptions_listsConfirmedWaitingAndCanceledWithMemberInfo() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        AppUser first = createUser("first", Role.MEMBER);
        AppUser second = createUser("second", Role.MEMBER);
        AppUser third = createUser("third", Role.MEMBER);
        Event event = createEvent(owner, 1, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        for (AppUser member : new AppUser[] { first, second, third }) {
            mockMvc.perform(post("/inscriptions")
                            .header("Authorization", "Bearer " + loginAndGetToken(member.getEmail()))
                            .param("userId", String.valueOf(member.getId()))
                            .param("eventId", String.valueOf(event.getId())))
                    .andExpect(status().isCreated());
        }

        String thirdToken = loginAndGetToken(third.getEmail());
        String thirdInscriptions = mockMvc.perform(get("/inscriptions/user/" + third.getId())
                        .header("Authorization", "Bearer " + thirdToken))
                .andReturn().getResponse().getContentAsString();
        Number thirdInscriptionId = (Number) objectMapper.readValue(thirdInscriptions, java.util.List.class)
                .stream().map(o -> ((Map<?, ?>) o).get("id")).findFirst().orElseThrow();
        mockMvc.perform(delete("/inscriptions/" + thirdInscriptionId.longValue())
                        .header("Authorization", "Bearer " + thirdToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/inscriptions/event/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.status=='CONFIRMED')].user.email").value(first.getEmail()))
                .andExpect(jsonPath("$[?(@.status=='WAITING_LIST')].user.email").value(second.getEmail()))
                .andExpect(jsonPath("$[?(@.status=='CANCELED')].user.email").value(third.getEmail()))
                .andExpect(jsonPath("$[0].inscriptionDate").exists())
                .andExpect(jsonPath("$[0].price").exists());
    }

    @Test
    void eventInscriptions_paginationIsApplied() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        Event event = createEvent(owner, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        for (int i = 0; i < 3; i++) {
            AppUser member = createUser("member" + i, Role.MEMBER);
            mockMvc.perform(post("/inscriptions")
                            .header("Authorization", "Bearer " + loginAndGetToken(member.getEmail()))
                            .param("userId", String.valueOf(member.getId()))
                            .param("eventId", String.valueOf(event.getId())))
                    .andExpect(status().isCreated());
        }
        String ownerToken = loginAndGetToken(owner.getEmail());

        mockMvc.perform(get("/inscriptions/event/" + event.getId()).param("size", "2").param("page", "0")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/inscriptions/event/" + event.getId()).param("size", "2").param("page", "1")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void register_eventFull_returns201WaitingList() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser firstMember = createUser("member1", Role.MEMBER);
        AppUser secondMember = createUser("member2", Role.MEMBER);
        Event event = createEvent(organizer, 1, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        String firstToken = loginAndGetToken(firstMember.getEmail());
        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + firstToken)
                        .param("userId", String.valueOf(firstMember.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        String secondToken = loginAndGetToken(secondMember.getEmail());
        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + secondToken)
                        .param("userId", String.valueOf(secondMember.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_LIST"));
    }

    @Test
    void register_alreadyRegistered_returns409() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void register_draftEvent_returns409() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.DRAFT);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void register_pastEvent_returns409() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().minusDays(1), EventStatus.PUBLISHED);
        String token = loginAndGetToken(member.getEmail());

        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // US-303 : promotion sur annulation
    // ---------------------------------------------------------------

    @Test
    void cancelConfirmedInscription_promotesFirstWaitingMember() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser confirmedMember = createUser("confirmed", Role.MEMBER);
        AppUser waitingMember = createUser("waiting", Role.MEMBER);
        Event event = createEvent(organizer, 1, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        String confirmedToken = loginAndGetToken(confirmedMember.getEmail());
        String confirmedResponse = mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + confirmedToken)
                        .param("userId", String.valueOf(confirmedMember.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long confirmedInscriptionId = ((Number) objectMapper.readValue(confirmedResponse, Map.class).get("id")).longValue();

        String waitingToken = loginAndGetToken(waitingMember.getEmail());
        mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + waitingToken)
                        .param("userId", String.valueOf(waitingMember.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_LIST"));

        mockMvc.perform(delete("/inscriptions/" + confirmedInscriptionId)
                        .header("Authorization", "Bearer " + confirmedToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/inscriptions/user/" + waitingMember.getId())
                        .header("Authorization", "Bearer " + waitingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    // ---------------------------------------------------------------
    // US-405 : annulation par l'organisateur
    // ---------------------------------------------------------------

    @Test
    void cancelByOrganizer_withoutMotif_returns400() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        String memberToken = loginAndGetToken(member.getEmail());
        String response = mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long inscriptionId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        String organizerToken = loginAndGetToken(organizer.getEmail());
        mockMvc.perform(delete("/inscriptions/" + inscriptionId + "/organizer")
                        .header("Authorization", "Bearer " + organizerToken)
                        .param("motif", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelByOrganizer_foreignOrganizer_returns403AndSendsNoEmail() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser stranger = createUser("stranger", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        String memberToken = loginAndGetToken(member.getEmail());
        String response = mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long inscriptionId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        String strangerToken = loginAndGetToken(stranger.getEmail());
        mockMvc.perform(delete("/inscriptions/" + inscriptionId + "/organizer")
                        .header("Authorization", "Bearer " + strangerToken)
                        .param("motif", "Tentative"))
                .andExpect(status().isForbidden());

        verify(emailService, org.mockito.Mockito.never()).sendInscriptionCancellationEmail(anyString(), anyString(), anyString());
    }

    // canceledById reste null meme apres une annulation organisateur : bug connu
    // documente au niveau unitaire (InscriptionServiceTest), confirme ici au niveau HTTP.
    @Test
    void cancelByOrganizer_sendsCancellationEmail() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser member = createUser("member", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);

        String memberToken = loginAndGetToken(member.getEmail());
        String response = mockMvc.perform(post("/inscriptions")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("userId", String.valueOf(member.getId()))
                        .param("eventId", String.valueOf(event.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long inscriptionId = ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();

        String organizerToken = loginAndGetToken(organizer.getEmail());
        mockMvc.perform(delete("/inscriptions/" + inscriptionId + "/organizer")
                        .header("Authorization", "Bearer " + organizerToken)
                        .param("motif", "Évènement annulé"))
                .andExpect(status().isNoContent());

        verify(emailService).sendInscriptionCancellationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void deleteEvent_withConfirmedAndWaitingInscriptions_removesThemWithTheEvent() throws Exception {
        AppUser organizer = createUser("organizer", Role.ORGANIZER);
        AppUser confirmed = createUser("confirmed", Role.MEMBER);
        AppUser waiting = createUser("waiting", Role.MEMBER);
        Event event = createEvent(organizer, 10, LocalDateTime.now().plusDays(5), EventStatus.PUBLISHED);
        inscriptionRepo.save(new Inscription(confirmed, event, BigDecimal.TEN, InscriptionStatus.CONFIRMED));
        inscriptionRepo.save(new Inscription(waiting, event, BigDecimal.TEN, InscriptionStatus.WAITING_LIST));

        mockMvc.perform(delete("/events/" + event.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(organizer.getEmail())))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(eventRepo.existsById(event.getId()));
        org.junit.jupiter.api.Assertions.assertTrue(
                inscriptionRepo.findByEventIdAndStatusIn(event.getId(),
                        java.util.List.of(InscriptionStatus.CONFIRMED, InscriptionStatus.WAITING_LIST)).isEmpty());
    }
}
