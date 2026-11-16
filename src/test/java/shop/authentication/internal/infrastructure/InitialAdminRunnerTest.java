package shop.authentication.internal.infrastructure;

import org.junit.jupiter.api.Test;
import shop.authentication.internal.domain.exception.IncompleteAdminConfigurationException;
import shop.authentication.internal.domain.service.AuthService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** The runner only hands the configuration to the service: the rule is the service's. */
class InitialAdminRunnerTest {

    private final AuthService authService = mock(AuthService.class);

    @Test
    void nothingHappensUntilTheApplicationRunsIt() {
        new InitialAdminRunner(authService, "root@correo.com", "Secreta123");

        verifyNoInteractions(authService);
    }

    @Test
    void onStartupPassesTheConfiguredValuesToTheService() {
        new InitialAdminRunner(authService, "root@correo.com", "Secreta123").run();

        verify(authService).ensureInitialAdmin("root@correo.com", "Secreta123");
    }

    @Test
    void blankValuesArePassedAsTheyAreSoTheServiceDecides() {
        new InitialAdminRunner(authService, "", "").run();

        verify(authService).ensureInitialAdmin("", "");
    }

    @Test
    void theFailureOfTheServiceStopsTheStartup() {
        doThrow(new IncompleteAdminConfigurationException()).when(authService).ensureInitialAdmin("root@correo.com", "");

        assertThrows(IncompleteAdminConfigurationException.class,
                () -> new InitialAdminRunner(authService, "root@correo.com", "").run());
    }
}
