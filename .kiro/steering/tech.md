# Tecnología

- Lenguaje: Java 26
- Build: Maven (`groupId` org.example, `artifactId` Peliculas)
- Framework: Spring Boot (API REST con JSON)
- Seguridad: Spring Security con autenticación JWT sin estado y contraseñas con BCrypt (usuario actual en `SecurityContextHolder`, basado en `ThreadLocal`)
- Validación: Bean Validation en los DTO de entrada
- Base de datos: MariaDB
- Codificación: UTF-8

## Pruebas
- JUnit 5 como framework de pruebas.
- Dobles de prueba escritos a mano (fakes/stubs); no se usa Mockito.
- Spring Modulith (`ApplicationModules.verify()`) comprueba en cada build los límites entre módulos.
- ArchUnit para validar reglas de arquitectura: `internal.domain` no depende de `usecase` ni `web`, los módulos solo se acceden por su paquete `api` y no hay ciclos entre módulos.

## Comandos
- Compilar: `mvn compile`
- Pruebas: `mvn test`
- Ejecutar: `mvn spring-boot:run` (cuando se añada el plugin de Spring Boot)

## Pendiente
- El `pom.xml` aún no declara Spring Boot, el driver de MariaDB, JUnit 5 ni ArchUnit.
