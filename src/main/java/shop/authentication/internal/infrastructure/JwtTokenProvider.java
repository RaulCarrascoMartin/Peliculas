package shop.authentication.internal.infrastructure;

import org.springframework.stereotype.Component;
import shop.authentication.internal.domain.contract.TokenProvider;
import shop.authentication.internal.domain.dto.UserDto;
import shop.shared.security.JwtTokens;

/** {@link TokenProvider} over the shared {@link JwtTokens}: the user id as subject and the role as claim. */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final JwtTokens tokens;

    public JwtTokenProvider(JwtTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public String generate(UserDto user) {
        return tokens.issueToken(user.id(), user.role().name());
    }
}
