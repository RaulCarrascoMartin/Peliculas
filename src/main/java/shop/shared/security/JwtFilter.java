package shop.shared.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <token>}, validates signature and expiry and fills the SecurityContext
 * with the id (principal) and the role, without querying the database. If the token is missing or invalid
 * the request continues unauthenticated, and the security chain decides whether the route requires it.
 */
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtTokens tokens;

    public JwtFilter(JwtTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            try {
                TokenData data = tokens.parseToken(header.substring(PREFIX.length()));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        data.id(), null, AuthorityUtils.createAuthorityList("ROLE_" + data.role())));
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
