package fr.diginamic.hubevenementiel.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.diginamic.hubevenementiel.logging.RequestLoggingFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LoggingIntegrationTest {

    private static final String UNKNOWN_EMAIL = "inconnu-logs@example.com";

    @Autowired
    private MockMvc mockMvc;

    private final Logger root = (Logger) LoggerFactory.getLogger("fr.diginamic.hubevenementiel");
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        root.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        root.detachAppender(appender);
    }

    @Test
    void everyResponseCarriesARequestId_andTheRequestIsLogged() throws Exception {
        mockMvc.perform(get("/events/filter").header(RequestLoggingFilter.REQUEST_ID_HEADER, "test-req-0001"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, "test-req-0001"));

        List<ILoggingEvent> lines = appender.list.stream()
                .filter(event -> event.getFormattedMessage().startsWith("GET /events/filter -> 200"))
                .toList();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getMDCPropertyMap()).containsEntry(RequestLoggingFilter.REQUEST_ID, "test-req-0001");
    }

    @Test
    void aFailedLogin_isLoggedAsAWarningWithoutTheEmailOrThePassword() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + UNKNOWN_EMAIL + "\",\"password\":\"mot-de-passe-secret\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).isEqualTo("Échec de connexion");
        });
        assertThat(appender.list).noneSatisfy(event -> assertThat(event.getFormattedMessage())
                .contains(UNKNOWN_EMAIL).contains("mot-de-passe-secret"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("POST /login -> 401"));
    }
}
