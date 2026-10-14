package fr.diginamic.hubevenementiel.config;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationInterceptorTest {

    private final PaginationInterceptor interceptor = new PaginationInterceptor();

    private boolean handle(String page, String size) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (page != null) {
            request.setParameter("page", page);
        }
        if (size != null) {
            request.setParameter("size", size);
        }
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    @Test
    void noPaginationParameters_isAccepted() throws Exception {
        assertThat(handle(null, null)).isTrue();
    }

    @Test
    void validValues_areAccepted() throws Exception {
        assertThat(handle("0", "1")).isTrue();
        assertThat(handle("3", "20")).isTrue();
    }

    @Test
    void negativePage_isRefused() {
        assertThrows(BadRequestException.class, () -> handle("-1", "20"));
    }

    @Test
    void zeroOrNegativeSize_isRefused() {
        assertThrows(BadRequestException.class, () -> handle("0", "0"));
        assertThrows(BadRequestException.class, () -> handle("0", "-5"));
    }

    @Test
    void nonNumericValue_isLeftToSpringTypeConversion() {
        assertDoesNotThrow(() -> handle("abc", "xyz"));
    }
}
