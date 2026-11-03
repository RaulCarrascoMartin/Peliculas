package shop.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import shop.authentication.internal.application.usecase.CreateAdminIfMissing;
import shop.authentication.internal.application.usecase.Login;
import shop.authentication.internal.application.usecase.RegisterUser;
import shop.authentication.internal.domain.service.AuthService;
import shop.authentication.internal.domain.service.AuthServiceImpl;
import shop.authentication.internal.domain.contract.PasswordHasher;
import shop.authentication.internal.domain.contract.TokenIssuer;
import shop.authentication.internal.domain.repository.UserRepository;
import shop.authentication.internal.infrastructure.BcryptPasswordHasher;
import shop.authentication.internal.infrastructure.JwtTokenIssuer;
import shop.authentication.internal.persistence.jpa.UserJpaRepository;
import shop.authentication.internal.persistence.repository.UserRepositoryImpl;
import shop.shared.security.JwtTokens;

@Configuration
class AuthenticationModuleConfig {

    @Bean
    UserRepository userRepository(UserJpaRepository dao) {
        return new UserRepositoryImpl(dao);
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder encoder) {
        return new BcryptPasswordHasher(encoder);
    }

    @Bean
    TokenIssuer tokenIssuer(JwtTokens tokens) {
        return new JwtTokenIssuer(tokens);
    }

    @Bean
    AuthService authService(UserRepository users, PasswordHasher hasher, TokenIssuer tokenIssuer) {
        return new AuthServiceImpl(users, hasher, tokenIssuer);
    }

    @Bean
    RegisterUser registerUser(AuthService authService) {
        return new RegisterUser(authService);
    }

    @Bean
    Login login(AuthService authService) {
        return new Login(authService);
    }

    @Bean
    CreateAdminIfMissing createAdminIfMissing(AuthService authService) {
        return new CreateAdminIfMissing(authService);
    }

    /** On startup creates the first admin from {@code peliculas.admin.email} and {@code peliculas.admin.contrasena}. */
    @Bean
    CommandLineRunner initialAdmin(CreateAdminIfMissing createAdmin,
                                   @Value("${peliculas.admin.email:}") String email,
                                   @Value("${peliculas.admin.contrasena:}") String password) {
        return args -> createAdmin.execute(email, password);
    }
}
