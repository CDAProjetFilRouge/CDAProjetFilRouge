package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.frontend-url=https://hub.example.org",
        "app.frontend-allowed-urls=https://autre.example.org" })
@AutoConfigureMockMvc
@Transactional
class FrontendUrlsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private JavaMailSender mailSender;

    private AppUser user;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("liens-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode("motdepasse123456"));
        user.setRole(Role.MEMBER);
        user.setStatus(AccountStatus.ACTIVE);
        userRepo.save(user);
    }

    private String htmlOfTheSentMail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return findPart(captor.getValue(), "text/html");
    }

    private static String findPart(Object content, String mimeType) throws Exception {
        if (content instanceof MimeMessage message) {
            message.saveChanges();
            if (message.isMimeType(mimeType)) {
                return (String) message.getContent();
            }
            return findPart(message.getContent(), mimeType);
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType(mimeType)) {
                    return (String) part.getContent();
                }
                String found = findPart(part.getContent(), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void resetLink_followsTheListedFrontThatAskedForIt() throws Exception {
        mockMvc.perform(post("/account/password/forgot").param("email", user.getEmail())
                        .header("Origin", "https://autre.example.org"))
                .andExpect(status().isOk());

        assertThat(htmlOfTheSentMail()).contains("href=\"https://autre.example.org/reset-password?token=");
    }

    @Test
    void resetLink_usesTheDefaultFrontWithoutOrigin() throws Exception {
        mockMvc.perform(post("/account/password/forgot").param("email", user.getEmail()))
                .andExpect(status().isOk());

        assertThat(htmlOfTheSentMail()).contains("href=\"https://hub.example.org/reset-password?token=");
    }

    @Test
    void anUnlistedOrigin_isRefusedBeforeAnyEmailIsSent() throws Exception {
        mockMvc.perform(post("/account/password/forgot").param("email", user.getEmail())
                        .header("Origin", "https://evil.example.com"))
                .andExpect(status().isForbidden());

        verify(mailSender, org.mockito.Mockito.never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void cors_acceptsTheDefaultAndTheListedFronts_andRefusesOthers() throws Exception {
        for (String origin : new String[] { "https://hub.example.org", "https://autre.example.org" }) {
            mockMvc.perform(options("/events/filter")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }

        mockMvc.perform(options("/events/filter")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
