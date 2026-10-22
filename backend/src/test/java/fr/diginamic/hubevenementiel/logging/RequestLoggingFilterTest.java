package fr.diginamic.hubevenementiel.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    private FilterChain respondingWith(int status) {
        return (request, response) -> ((MockHttpServletResponse) response).setStatus(status);
    }

    @Test
    void generatesARequestId_returnsItInTheResponse_andClearsTheContextAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events");
        request.setRemoteAddr("203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] seenDuringRequest = new String[2];
        FilterChain chain = (req, res) -> {
            seenDuringRequest[0] = MDC.get(RequestLoggingFilter.REQUEST_ID);
            seenDuringRequest[1] = MDC.get(RequestLoggingFilter.CLIENT_IP);
        };

        filter.doFilter(request, response, chain);

        String id = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(id).isNotBlank();
        assertThat(seenDuringRequest[0]).isEqualTo(id);
        assertThat(seenDuringRequest[1]).isEqualTo("203.0.113.7");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void keepsAWellFormedIncomingRequestId_butReplacesAMalformedOne() throws Exception {
        MockHttpServletRequest good = new MockHttpServletRequest("GET", "/events");
        good.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "front-1234-abcd");
        MockHttpServletResponse goodResponse = new MockHttpServletResponse();
        filter.doFilter(good, goodResponse, respondingWith(200));
        assertThat(goodResponse.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("front-1234-abcd");

        MockHttpServletRequest forged = new MockHttpServletRequest("GET", "/events");
        forged.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "abc\nINFO faux log injecté");
        MockHttpServletResponse forgedResponse = new MockHttpServletResponse();
        filter.doFilter(forged, forgedResponse, respondingWith(200));
        assertThat(forgedResponse.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
                .doesNotContain("\n")
                .isNotEqualTo("abc\nINFO faux log injecté");
    }

    @Test
    void logsOneLinePerRequest_withLevelDependingOnTheStatus_andNeverTheQueryString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/events/1/comments");
        request.setQueryString("content=un texte saisi par l'utilisateur");

        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(201));
        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(403));
        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(429));
        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(409));
        filter.doFilter(request, new MockHttpServletResponse(), respondingWith(500));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.WARN, Level.WARN, Level.INFO, Level.ERROR);
        assertThat(appender.list.get(0).getFormattedMessage())
                .startsWith("POST /events/1/comments -> 201 (")
                .endsWith(" ms)")
                .doesNotContain("texte saisi");
    }

    @Test
    void logsTheUserWhenAuthenticatedDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events/mine");
        FilterChain chain = (req, res) -> MDC.put(RequestLoggingFilter.USER_ID, "user-42");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getMDCPropertyMap()).containsEntry(RequestLoggingFilter.USER_ID, "user-42");
        assertThat(MDC.get(RequestLoggingFilter.USER_ID)).isNull();
    }

    @Test
    void logsAnUnexpectedExceptionWithItsStackTrace_rethrowsIt_andStillClearsTheContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/events/1");
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom", new IllegalStateException("cause"));
        };

        assertThrows(ServletException.class, () -> filter.doFilter(request, new MockHttpServletResponse(), chain));

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getMDCPropertyMap()).containsKey(RequestLoggingFilter.REQUEST_ID);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void ignoresPreflightAndUploadsRequests() throws IOException, ServletException {
        MockHttpServletResponse preflight = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/events"), preflight, new MockFilterChain());
        MockHttpServletResponse upload = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/uploads/photo.jpg"), upload, new MockFilterChain());

        assertThat(appender.list).isEmpty();
        assertThat(preflight.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNull();
        assertThat(upload.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNull();
    }
}
