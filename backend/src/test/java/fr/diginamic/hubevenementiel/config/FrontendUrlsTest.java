package fr.diginamic.hubevenementiel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class FrontendUrlsTest {

    private final FrontendUrls urls = new FrontendUrls("https://hub.example.org/",
            " https://autre.example.org , http://localhost:4200/ ,, ");

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void requestFrom(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void allowedOrigins_areTheDefaultAndTheListedOnesWithoutTrailingSlash() {
        assertThat(urls.allowedOrigins()).containsExactly(
                "https://hub.example.org", "https://autre.example.org", "http://localhost:4200");
        assertThat(urls.defaultUrl()).isEqualTo("https://hub.example.org");
    }

    @Test
    void current_followsTheOriginOfTheRequestWhenItIsListed() {
        requestFrom("https://autre.example.org");
        assertThat(urls.current()).isEqualTo("https://autre.example.org");

        requestFrom("HTTPS://Autre.Example.org/");
        assertThat(urls.current()).isEqualTo("https://autre.example.org");
    }

    @Test
    void current_fallsBackToTheDefaultForAnUnknownOrigin() {
        requestFrom("https://evil.example.com");
        assertThat(urls.current()).isEqualTo("https://hub.example.org");
    }

    @Test
    void current_isNotFooledByLookalikeOrigins() {
        for (String forged : new String[] { "https://hub.example.org.evil.com", "http://hub.example.org",
                "https://hub.example.org:8443", "https://evilhub.example.org", "https://autre.example.org@evil.com",
                "null", "" }) {
            requestFrom(forged);
            assertThat(urls.current()).as(forged).isEqualTo("https://hub.example.org");
        }
    }

    @Test
    void current_usesTheDefaultWithoutOriginHeaderOrOutsideARequest() {
        requestFrom(null);
        assertThat(urls.current()).isEqualTo("https://hub.example.org");

        RequestContextHolder.resetRequestAttributes();
        assertThat(urls.current()).isEqualTo("https://hub.example.org");
    }
}
