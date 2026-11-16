package shop.shared;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;

/**
 * Base of the repository tests: a {@code @JdbcTest} slice against a real MariaDB (Testcontainers), the same engine
 * as production, with the schema created by the Flyway migrations. The container is started once and shared by
 * every repository test of the build; each test runs in a transaction that is rolled back. Requires Docker.
 *
 * <p>Subclasses only add {@code @Import(XRepositoryImpl.class)}.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class MariaDbRepositoryTest {

    private static final MariaDBContainer DATABASE = new MariaDBContainer("mariadb:11");

    static {
        DATABASE.start();
    }

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }
}
