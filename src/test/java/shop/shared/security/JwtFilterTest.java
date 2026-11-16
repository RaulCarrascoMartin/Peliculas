package shop.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtFilterTest {

    private JwtTokens tokens;
    private JwtFilter filter;
    private FilterChain chain;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        tokens = new JwtTokens("secreto-de-pruebas-con-longitud-suficiente-para-hs256-0123456789", java.time.Duration.ofHours(1));
        filter = new JwtFilter(tokens);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    void validTokenFillsTheContextWithIdAndRoleAndContinues() throws Exception {
        MockHttpServletRequest request = requestWith("Bearer " + tokens.issueToken(7L, "ADMIN"));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(7L, authentication.getPrincipal());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingOrInvalidTokenLeavesTheContextEmptyAndContinues() throws Exception {
        for (String header : new String[] {null, "Bearer no-es-un-token", "Basic abc"}) {
            MockHttpServletRequest request = requestWith(header);

            filter.doFilter(request, response, chain);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(chain).doFilter(request, response);
        }
    }
}
