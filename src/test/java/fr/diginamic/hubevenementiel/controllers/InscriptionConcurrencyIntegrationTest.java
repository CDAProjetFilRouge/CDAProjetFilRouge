package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PAS de @Transactional de classe ici : les fixtures (event, membres) doivent etre
// reellement commitees en base pour etre visibles depuis les deux threads qui
// appellent l'API en parallele (chacun dans sa propre transaction HTTP). Nettoyage
// manuel en @AfterEach a la place du rollback automatique.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InscriptionConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EventRepo eventRepo;
    @Autowired
    private InscriptionRepo inscriptionRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String PASSWORD = "motdepasse123456";

    private Long eventId;
    private Long organizerId;
    private final List<Long> memberIds = new java.util.ArrayList<>();

    @AfterEach
    void cleanUp() {
        inscriptionRepo.findAll().stream()
                .filter(i -> i.getEvent() != null && i.getEvent().getId().equals(eventId))
                .forEach(inscriptionRepo::delete);
        if (eventId != null) {
            eventRepo.findById(eventId).ifPresent(eventRepo::delete);
        }
        memberIds.forEach(id -> userRepo.findById(id).ifPresent(userRepo::delete));
        if (organizerId != null) {
            userRepo.findById(organizerId).ifPresent(userRepo::delete);
        }
    }

    private AppUser createUser(String prefix, Role role) {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setClubs(List.of());
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

    // Le test le plus important du backlog EP3 : deux inscriptions simultanees sur
    // la toute derniere place ne doivent jamais produire deux CONFIRMED. S'appuie
    // sur le verrou pessimiste deja present (EventRepo.findByIdForUpdate).
    @Test
    void twoSimultaneousRegistrations_onLastSpot_neverBothConfirmed() throws Exception {
        AppUser organizer = createUser("organizer-conc", Role.ORGANIZER);
        organizerId = organizer.getId();

        Event event = new Event();
        event.setTitle("Event concurrence-" + System.nanoTime());
        event.setDescription("desc");
        event.setCategory(Category.SPORT);
        event.setStartDateTime(LocalDateTime.now().plusDays(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(5).plusHours(2));
        event.setMaxCapacity(1);
        event.setAffiliatePrice(BigDecimal.TEN);
        event.setNonAffiliatePrice(BigDecimal.valueOf(20));
        event.setStatus(EventStatus.PUBLISHED);
        event.setOrganizer(organizer);
        event = eventRepo.save(event);
        eventId = event.getId();
        Long finalEventId = eventId;

        AppUser memberA = createUser("member-a-conc", Role.MEMBER);
        AppUser memberB = createUser("member-b-conc", Role.MEMBER);
        memberIds.add(memberA.getId());
        memberIds.add(memberB.getId());

        String tokenA = loginAndGetToken(memberA.getEmail());
        String tokenB = loginAndGetToken(memberB.getEmail());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger createdCount = new AtomicInteger(0);

        Runnable taskA = registerTask(memberA.getId(), finalEventId, tokenA, readyLatch, startLatch, createdCount);
        Runnable taskB = registerTask(memberB.getId(), finalEventId, tokenB, readyLatch, startLatch, createdCount);

        executor.submit(taskA);
        executor.submit(taskB);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(createdCount.get()).isEqualTo(2);

        List<Inscription> inscriptions = inscriptionRepo.findAll().stream()
                .filter(i -> i.getEvent().getId().equals(finalEventId))
                .toList();

        long confirmedCount = inscriptions.stream().filter(i -> i.getStatus() == InscriptionStatus.CONFIRMED).count();
        long waitingCount = inscriptions.stream().filter(i -> i.getStatus() == InscriptionStatus.WAITING_LIST).count();

        assertThat(confirmedCount).isEqualTo(1);
        assertThat(waitingCount).isEqualTo(1);
    }

    private Runnable registerTask(Long userId, Long eventId, String token, CountDownLatch readyLatch,
            CountDownLatch startLatch, AtomicInteger createdCount) {
        return () -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                int status = mockMvc.perform(post("/inscriptions")
                                .header("Authorization", "Bearer " + token)
                                .param("userId", String.valueOf(userId))
                                .param("eventId", String.valueOf(eventId)))
                        .andReturn().getResponse().getStatus();
                if (status == 201) {
                    createdCount.incrementAndGet();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
