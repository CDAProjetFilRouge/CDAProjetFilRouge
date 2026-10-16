package fr.diginamic.hubevenementiel.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.search-per-minute=3",
        "app.security.rate-limit.sensitive-per-minute=2",
        "app.security.rate-limit.pdf-per-minute=2",
        "app.security.rate-limit.default-per-minute=1000"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    private static final AtomicInteger IP_COUNTER = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    private String uniqueIp() {
        int n = IP_COUNTER.incrementAndGet();
        return "10.77." + (n / 250) + "." + (n % 250);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String ip) throws Exception {
        return mockMvc.perform(request.header("X-Forwarded-For", ip));
    }

    @Test
    void searchIsRefusedAfterItsLimitWithRetryAfter() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 3; i++) {
            call(get("/events/filter"), ip).andExpect(status().isOk());
        }

        call(get("/events/filter"), ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-Error-Code", "TOO_MANY_REQUESTS"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void anotherIpIsNotAffected() throws Exception {
        String attacker = uniqueIp();
        String other = uniqueIp();

        for (int i = 0; i < 4; i++) {
            call(get("/events/filter"), attacker);
        }

        call(get("/events/filter"), other).andExpect(status().isOk());
    }

    @Test
    void sensitiveRoutesHaveTheirOwnStricterLimit() throws Exception {
        String ip = uniqueIp();
        String body = "{\"email\":\"inconnu@example.com\"}";

        for (int i = 0; i < 2; i++) {
            call(post("/account/password/forgot").contentType(MediaType.APPLICATION_JSON).content(body), ip)
                    .andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }

        call(post("/account/password/forgot").contentType(MediaType.APPLICATION_JSON).content(body), ip)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void policiesDoNotShareTheirCounters() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 4; i++) {
            call(get("/events/filter"), ip);
        }

        call(get("/legal-documents/TERM_OF_USE"), ip).andExpect(status().is(org.hamcrest.Matchers.not(429)));
    }

    @Test
    void preflightRequestsAreNeverLimited() throws Exception {
        String ip = uniqueIp();

        for (int i = 0; i < 10; i++) {
            call(options("/events/filter")
                    .header("Origin", "http://localhost:4200")
                    .header("Access-Control-Request-Method", "GET"), ip)
                    .andExpect(status().is(org.hamcrest.Matchers.not(429)));
        }
    }
}
