package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class LoginRateLimitIntegrationTest {

    private static final String PASSWORD = "motdepasse123456";
    private static final String WRONG_PASSWORD = "mauvais-mot-de-passe";
    private static final AtomicInteger IP_COUNTER = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String createUser(AccountStatus status) {
        String email = "ratelimit-" + System.nanoTime() + "@example.com";
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.MEMBER);
        user.setStatus(status);
        userRepo.save(user);
        return email;
    }

    private String uniqueIp() {
        int n = IP_COUNTER.incrementAndGet();
        return "10.99." + (n / 250) + "." + (n % 250);
    }

    private ResultActions login(String email, String password, String ip) throws Exception {
        return mockMvc.perform(post("/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    @Test
    void sixthAttemptIsRefusedWith429EvenWithTheRightPassword() throws Exception {
        String email = createUser(AccountStatus.ACTIVE);
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            login(email, WRONG_PASSWORD, ip).andExpect(status().isUnauthorized());
        }

        login(email, PASSWORD, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Error-Code", "TOO_MANY_ATTEMPTS"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void anotherAccountOnTheSameIpStillLogsIn() throws Exception {
        String attacked = createUser(AccountStatus.ACTIVE);
        String other = createUser(AccountStatus.ACTIVE);
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            login(attacked, WRONG_PASSWORD, ip).andExpect(status().isUnauthorized());
        }

        login(other, PASSWORD, ip).andExpect(status().isOk());
    }

    @Test
    void successfulLoginResetsTheCounter() throws Exception {
        String email = createUser(AccountStatus.ACTIVE);
        String ip = uniqueIp();

        for (int i = 0; i < 4; i++) {
            login(email, WRONG_PASSWORD, ip).andExpect(status().isUnauthorized());
        }
        login(email, PASSWORD, ip).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(email, WRONG_PASSWORD, ip).andExpect(status().isUnauthorized());
        }

        login(email, PASSWORD, ip).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordOnASuspendedAccountIsCountedToo() throws Exception {
        String email = createUser(AccountStatus.SUSPENDED);
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            int httpStatus = login(email, WRONG_PASSWORD, ip).andReturn().getResponse().getStatus();
            assertThat(httpStatus).isIn(401, 403);
        }

        login(email, WRONG_PASSWORD, ip).andExpect(status().isTooManyRequests());
    }
}
