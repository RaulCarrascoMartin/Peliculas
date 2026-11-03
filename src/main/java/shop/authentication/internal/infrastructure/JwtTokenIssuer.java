package shop.authentication.internal.infrastructure;

import shop.authentication.internal.domain.contract.TokenIssuer;
import shop.shared.security.JwtTokens;

/** {@link TokenIssuer} over the shared {@link JwtTokens}. */
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtTokens tokens;

    public JwtTokenIssuer(JwtTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public String issue(Long userId, String role) {
        return tokens.issueToken(userId, role);
    }
}
