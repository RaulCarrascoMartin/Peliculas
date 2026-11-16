package shop.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 401 and 403 produced by the security chain leave as ProblemDetail, without leaking the cause. */
class ProblemSecurityHandlersTest {

    @Test
    void entryPointWrites401AsProblemDetail() throws Exception {
        var response = new MockHttpServletResponse();

        new ProblemAuthenticationEntryPoint()
                .commence(new MockHttpServletRequest(), response, new BadCredentialsException("token expired for user 7"));

        assertEquals(401, response.getStatus());
        assertEquals("application/problem+json;charset=UTF-8", response.getContentType());
        assertEquals("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"No autenticado\"}",
                response.getContentAsString());
    }

    @Test
    void accessDeniedHandlerWrites403AsProblemDetail() throws Exception {
        var response = new MockHttpServletResponse();

        new ProblemAccessDeniedHandler()
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("needs ROLE_ADMIN"));

        assertEquals(403, response.getStatus());
        assertEquals("application/problem+json;charset=UTF-8", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"status\":403"));
        assertTrue(response.getContentAsString().contains("\"detail\":\"Acceso denegado\""));
        assertFalse(response.getContentAsString().contains("ROLE_ADMIN"));
    }
}
