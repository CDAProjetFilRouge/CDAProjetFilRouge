package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Image;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.ImageRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class ImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EventRepo eventRepo;
    @Autowired
    private ClubRepo clubRepo;
    @Autowired
    private ImageRepo imageRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PASSWORD = "motdepasse123456";

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

    private Event createEvent(AppUser organizer) {
        Club club = new Club();
        club.setName("Club-" + System.nanoTime());
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        Event event = new Event();
        event.setTitle("Event-" + System.nanoTime());
        event.setDescription("desc");
        event.setCategory(Category.SPORT);
        event.setStartDateTime(LocalDateTime.now().plusDays(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(5).plusHours(2));
        event.setMaxCapacity(10);
        event.setAffiliatePrice(BigDecimal.TEN);
        event.setNonAffiliatePrice(BigDecimal.valueOf(20));
        event.setStatus(EventStatus.PUBLISHED);
        event.setOrganizer(organizer);
        event.setClub(clubRepo.save(club));
        return eventRepo.save(event);
    }

    private Image createImage(Event event) {
        Image image = new Image();
        image.setFileName("photo.jpg");
        image.setPath("/uploads/00000000-0000-0000-0000-000000000000.jpg");
        image.setMimeType("image/jpeg");
        image.setSizeByte(3L);
        image.setUploadDate(LocalDateTime.now());
        image.setDisplayOrder(0);
        image.setEvent(event);
        return imageRepo.save(image);
    }

    private String loginAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (String) objectMapper.readValue(response, Map.class).get("token");
    }

    @Test
    void upload_foreignOrganizer_returns403AndStoresNothing() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        AppUser stranger = createUser("stranger", Role.ORGANIZER);
        Event event = createEvent(owner);
        long before = imageRepo.countByEventId(event.getId());

        mockMvc.perform(multipart("/events/" + event.getId() + "/images")
                        .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}))
                        .header("Authorization", "Bearer " + loginAndGetToken(stranger.getEmail())))
                .andExpect(status().isForbidden());

        assertThat(imageRepo.countByEventId(event.getId())).isEqualTo(before);
    }

    @Test
    void delete_foreignOrganizer_returns403AndKeepsTheImage() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        AppUser stranger = createUser("stranger", Role.ORGANIZER);
        Event event = createEvent(owner);
        Image image = createImage(event);

        mockMvc.perform(delete("/events/" + event.getId() + "/images/" + image.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(stranger.getEmail())))
                .andExpect(status().isForbidden());

        assertThat(imageRepo.findById(image.getId())).isPresent();
    }

    @Test
    void delete_imageOfAnotherEvent_returns404EvenForTheOwnerOfTheUrlEvent() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        AppUser other = createUser("other", Role.ORGANIZER);
        Event mine = createEvent(owner);
        Event theirs = createEvent(other);
        Image theirImage = createImage(theirs);

        mockMvc.perform(delete("/events/" + mine.getId() + "/images/" + theirImage.getId())
                        .header("Authorization", "Bearer " + loginAndGetToken(owner.getEmail())))
                .andExpect(status().isNotFound());

        assertThat(imageRepo.findById(theirImage.getId())).isPresent();
    }

    // Reproduction HTTP du constat : un SVG contenant du script, annoncé comme image, est refusé.
    @Test
    void upload_svgWithScript_returns400AndStoresNothing() throws Exception {
        AppUser owner = createUser("owner", Role.ORGANIZER);
        Event event = createEvent(owner);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(document.domain)</script></svg>".getBytes();

        mockMvc.perform(multipart("/events/" + event.getId() + "/images")
                        .file(new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg))
                        .header("Authorization", "Bearer " + loginAndGetToken(owner.getEmail())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/events/" + event.getId() + "/images")
                        .file(new MockMultipartFile("file", "logo.png", "image/png", svg))
                        .header("Authorization", "Bearer " + loginAndGetToken(owner.getEmail())))
                .andExpect(status().isBadRequest());

        assertThat(imageRepo.countByEventId(event.getId())).isZero();
    }

    // Les fichiers publics sont servis avec X-Content-Type-Options: nosniff.
    @Test
    void uploadedFiles_areServedWithNosniff() throws Exception {
        Path dir = Path.of(uploadDir);
        Files.createDirectories(dir);
        Path file = dir.resolve("00000000-0000-0000-0000-00000000aaaa.jpg");
        Files.write(file, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01});
        try {
            mockMvc.perform(get("/uploads/" + file.getFileName()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
