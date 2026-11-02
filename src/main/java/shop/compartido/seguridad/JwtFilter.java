package shop.compartido.seguridad;

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
 * Lee {@code Authorization: Bearer <token>}, valida firma y caducidad y rellena el SecurityContext
 * con el id (principal) y el rol, sin consultar la base de datos. Si el token falta o no es válido
 * la petición sigue sin autenticar, y la cadena de seguridad decide si la ruta lo exige.
 */
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtTokens tokens;

    public JwtFilter(JwtTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                DatosToken datos = tokens.leer(cabecera.substring(PREFIJO.length()));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        datos.id(), null, AuthorityUtils.createAuthorityList("ROLE_" + datos.rol())));
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
