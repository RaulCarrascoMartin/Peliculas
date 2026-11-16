# Tecnología

- Lenguaje: Java 26
- Build: Maven (`groupId` org.example, `artifactId` Peliculas)
- Framework: Spring Boot (API REST con JSON)
- Seguridad: Spring Security con autenticación JWT sin estado y contraseñas con BCrypt (usuario actual en `SecurityContextHolder`, basado en `ThreadLocal`)
- Validación: Bean Validation en los DTO web de entrada
- Persistencia: JDBC con `JdbcClient` (sin JPA); esquema con migraciones Flyway
- Mapeo entre DTO y modelos: MapStruct
- Contratos HTTP: OpenAPI (generación de interfaces y DTO con openapi-generator; anotaciones `swagger-annotations` en los contratos escritos a mano)
- Errores HTTP: `ProblemDetail` (RFC 9457)
- Base de datos: MariaDB
- Codificación: UTF-8

## Pruebas
- JUnit 5 como framework de pruebas.
- Mockito para los dobles de repositorios y servicios (`@MockitoBean` en `@WebMvcTest`).
- Testcontainers (MariaDB) para las pruebas de repositorio; requieren Docker en marcha.
- H2 en modo MariaDB para las pruebas de integración `@SpringBootTest`.
- Spring Modulith (`ApplicationModules.verify()`) comprueba en cada build los límites entre módulos.
- ArchUnit valida las reglas de capas y de ubicación de clases dentro de cada módulo.

## Comandos
- Compilar: `mvn compile`
- Pruebas: `mvn test`
- Ejecutar: `mvn spring-boot:run`
