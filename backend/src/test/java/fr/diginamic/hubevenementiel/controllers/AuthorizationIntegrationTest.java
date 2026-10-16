package fr.diginamic.hubevenementiel.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role checks on the sensitive routes: the role must come from the database on every request,
 * and each administration route must refuse non-administrators.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AuthorizationIntegrationTest {

    private static final String PASSWORD = "motdepasse123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailService emailService;

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
        user.setClubs(List.of());
        return userRepo.save(user);
    }

    private String tokenOf(AppUser user) throws Exception {
        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (String) objectMapper.readValue(response, Map.class).get("token");
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
    }

    // ---------------------------------------------------------------
    // The role is read from the database, not from the token
    // ---------------------------------------------------------------

    @Test
    void demotedAdministrator_losesAdminRights_withTheSameToken() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = tokenOf(admin);

        mockMvc.perform(withToken(get("/users"), token)).andExpect(status().isOk());

        admin.setRole(Role.MEMBER);
        userRepo.saveAndFlush(admin);

        mockMvc.perform(withToken(get("/users"), token)).andExpect(status().isForbidden());
    }

    @Test
    void promotedMember_getsNewRights_withTheSameToken() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        String token = tokenOf(member);

        mockMvc.perform(withToken(get("/users"), token)).andExpect(status().isForbidden());

        member.setRole(Role.ADMINISTRATOR);
        userRepo.saveAndFlush(member);

        mockMvc.perform(withToken(get("/users"), token)).andExpect(status().isOk());
    }

    @Test
    void suspendedUser_isRefused_withTheSameToken() throws Exception {
        AppUser admin = createUser("admin", Role.ADMINISTRATOR);
        String token = tokenOf(admin);

        admin.setStatus(AccountStatus.SUSPENDED);
        userRepo.saveAndFlush(admin);

        mockMvc.perform(withToken(get("/users"), token)).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Administration routes: member and organizer get 403
    // ---------------------------------------------------------------

    @Test
    void administrationRoutes_refuseMemberAndOrganizer() throws Exception {
        UUID id = UUID.randomUUID();

        for (Role role : new Role[] { Role.MEMBER, Role.ORGANIZER }) {
            String token = tokenOf(createUser("caller", role));

            for (MockHttpServletRequestBuilder request : List.of(
                    get("/users"),
                    get("/users/" + id),
                    post("/users/admin"),
                    put("/users/" + id + "/suspend"),
                    put("/users/" + id + "/reactivate"),
                    put("/users/" + id + "/admin"),
                    delete("/users/" + id),
                    get("/clubs/admin"),
                    post("/clubs"),
                    put("/clubs/" + id),
                    delete("/clubs/" + id),
                    get("/anonymization-demands"),
                    get("/anonymization-demands/count"),
                    put("/anonymization-demands/" + id + "/validate"))) {
                mockMvc.perform(withToken(request, token)).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void organizerRoutes_refuseMember() throws Exception {
        UUID id = UUID.randomUUID();
        String token = tokenOf(createUser("member", Role.MEMBER));

        for (MockHttpServletRequestBuilder request : List.of(
                get("/events/mine"),
                post("/events"),
                put("/events/" + id),
                delete("/events/" + id),
                delete("/inscriptions/" + id + "/organizer"))) {
            mockMvc.perform(withToken(request, token)).andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // Resources of someone else
    // ---------------------------------------------------------------

    @Test
    void member_cannotReadOrEditAnotherAccount() throws Exception {
        AppUser member = createUser("member", Role.MEMBER);
        AppUser stranger = createUser("stranger", Role.MEMBER);
        String token = tokenOf(member);

        mockMvc.perform(withToken(get("/inscriptions/user/" + stranger.getId()), token))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/users/" + stranger.getId()), token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequests_areRefusedOnProtectedRoutes() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/dashboard/kpis")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/inscriptions/user/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}
