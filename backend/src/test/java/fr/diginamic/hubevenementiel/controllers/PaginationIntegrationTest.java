package fr.diginamic.hubevenementiel.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class PaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PASSWORD = "motdepasse123456";

    private String adminToken() throws Exception {
        AppUser admin = new AppUser();
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setEmail("admin-" + System.nanoTime() + "@example.com");
        admin.setPhone("0600000000");
        admin.setHashedPassword(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMINISTRATOR);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setClubs(List.of());
        userRepo.save(admin);
        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", admin.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (String) objectMapper.readValue(response, Map.class).get("token");
    }

    @Test
    void invalidPagination_isRefusedWith400OnPublicRoutes() throws Exception {
        for (String path : List.of("/events", "/events/filter", "/clubs")) {
            mockMvc.perform(get(path).param("size", "0")).andExpect(status().isBadRequest());
            mockMvc.perform(get(path).param("page", "-1")).andExpect(status().isBadRequest());
        }
    }

    @Test
    void invalidPagination_isRefusedWith400OnAuthenticatedRoutes() throws Exception {
        String token = adminToken();
        for (String path : List.of("/users", "/events/mine")) {
            mockMvc.perform(get(path).param("size", "0").header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(path).param("page", "-1").header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void validPagination_stillWorks() throws Exception {
        mockMvc.perform(get("/events/filter").param("page", "0").param("size", "5")).andExpect(status().isOk());
    }
}
