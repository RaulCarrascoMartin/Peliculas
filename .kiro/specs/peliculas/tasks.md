# Plan de implementación

- [ ] 1. Preparar el proyecto
  - [x] 1.1 Añadir al `pom.xml` Spring Boot (web, data-jpa, security, validation), driver de MariaDB, librería JWT, JUnit 5 y ArchUnit
  - [x] 1.2 Configurar `application.properties` (MariaDB, secreto y caducidad del JWT por variables de entorno)
  - [x] 1.3 Convertir `Main` en la clase de arranque de Spring Boot
  - [x] 1.4 Escribir las pruebas de ArchUnit (`domain` sin dependencias hacia `application`/`web`, acceso entre módulos solo por `Api`, sin ciclos, `compartido` sin dependencias de módulos)
  - _Requisitos: todos_

- [ ] 2. Infraestructura común (`compartido`)
  - [x] 2.1 Crear las excepciones de negocio (`RecursoNoEncontradoException`, `ConflictoException`)
  - [x] 2.2 Implementar `GlobalExceptionHandler` con el cuerpo de error uniforme y la tabla de estados 400/401/403/404/409
  - [x] 2.3 Implementar `UsuarioActual` sobre `SecurityContextHolder`, con su doble de prueba escrito a mano
  - [x] 2.4 Pruebas del manejo de errores
  - _Requisitos: 0.4, 0.5, 1.5, 1.6_

- [ ] 3. Módulo `autenticacion`
  - [x] 3.1 Entidad anémica `Usuario` y `UsuarioRepository`
  - [x] 3.2 `AutenticacionService`: registro con BCrypt, rechazo de email duplicado, login y emisión del JWT
  - [x] 3.3 `RegistroRequest`, `LoginRequest`, `TokenResponse` y `AutenticacionController`
  - [x] 3.4 `JwtFilter` y `SecurityConfig` (sin sesión, rutas públicas y protegidas, `@PreAuthorize` por rol)
  - [x] 3.5 Crear el primer administrador al arrancar a partir de la configuración
  - [x] 3.6 Pruebas unitarias del servicio y de integración de seguridad (sin token, caducado, firma alterada, rol insuficiente)
  - _Requisitos: 0.1, 0.2, 0.3, 0.4, 0.5_

- [ ] 4. Módulo `pelicula`: catálogo
  - [x] 4.1 Entidad anémica `Pelicula` y `PeliculaRepository`
  - [x] 4.2 `PeliculaService`: alta, edición, baja y detalle, con error 404 si no existe
  - [x] 4.3 `PeliculaRequest` (con validaciones), `PeliculaResponse` y `PeliculaController`, con creación, edición y baja solo para administrador
  - [x] 4.4 `PeliculaApi` con `existe(id)` y actualización de la valoración media
  - [x] 4.5 Pruebas unitarias del servicio e integración del controlador
  - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2_

- [ ] 5. Módulo `pelicula`: consulta, búsqueda y filtros
  - [x] 5.1 Listado paginado
  - [x] 5.2 Búsqueda por título sin distinguir mayúsculas y filtros por género, año y director combinables
  - [x] 5.3 Lista vacía cuando no hay resultados
  - [x] 5.4 Pruebas del servicio y del controlador
  - _Requisitos: 2.1, 3.1, 3.2, 3.3_

- [ ] 6. Módulo `valoracion`: valoraciones
  - [x] 6.1 Entidad anémica `Valoracion`, restricción única (usuario, película) y `ValoracionRepository`
  - [x] 6.2 `ValoracionService`: crear o actualizar la valoración del usuario actual, validar el rango 1-5 y comprobar que la película existe con `PeliculaApi`
  - [x] 6.3 Recalcular la media y guardarla mediante `PeliculaApi`
  - [x] 6.4 `ValoracionRequest`, `ValoracionResponse` y `ValoracionController`
  - [x] 6.5 Pruebas con dobles escritos a mano de `PeliculaApi` y `UsuarioActual`
  - _Requisitos: 4.1, 4.2, 4.3, 4.4_

- [ ] 7. Módulo `valoracion`: favoritas y vistas
  - [x] 7.1 Entidad anémica `MarcaPelicula` y su repositorio
  - [x] 7.2 Servicio para marcar y desmarcar películas como favoritas o vistas
  - [x] 7.3 Endpoints para marcar, desmarcar y consultar las propias listas
  - [x] 7.4 Pruebas del servicio y del controlador
  - _Requisitos: 5.1, 5.2, 5.3_

- [ ] 8. Borrado coordinado entre módulos
  - [x] 8.1 Decidir cómo `valoracion` limpia sus datos al borrar una película sin crear un ciclo entre módulos (propuesta: `pelicula` publica un evento `PeliculaEliminada` y `valoracion` lo escucha)
  - [x] 8.2 Implementar el borrado de valoraciones y marcas
  - [ ] 8.3 Prueba de integración del borrado
  - _Requisitos: 1.4_

- [ ] 9. Cierre
  - [ ] 9.1 Revisar la cobertura de requisitos con las pruebas de integración
  - [ ] 9.2 Ejecutar `mvn test` completo, incluidas las pruebas de ArchUnit

## Implementation Notes
- 3.2: `JwtTokens` (emitir/leer, en `compartido`) lo reutiliza `JwtFilter` en 3.4; `AutenticacionService` necesita un bean `PasswordEncoder` (BCrypt) (ya definido en `SecurityConfig`).
- Dobles de repositorio: extender `RepositorioEnMemoria<T>` (src/test/.../compartido) y añadir solo las consultas derivadas necesarias.
- Controladores: no usar `@Validated` a nivel de clase (rompe el 400 de `HandlerMethodValidationException`).
- 3.4: `JwtFilter` se instancia dentro de `SecurityConfig` y no es bean (así Boot no lo registra dos veces). Rutas públicas: POST registro/login y GET `/api/peliculas` y `/api/peliculas/*`; cualquier ruta nueva pública hay que añadirla ahí. `SeguridadIntegracionTest` ya cubre sin token, caducado, firma alterada, otro secreto y rol insuficiente (adelanta parte de 3.6).
- 5.1: el listado devuelve `PagedModel` (`content` + `page`), orden por defecto por id; el doble `PeliculaRepositoryFalso` implementa `findAll(Pageable)`.
- 5.2: `PeliculaRepository.buscar` (JPQL con parámetros opcionales) sustituye a `listar`; `PeliculaService.buscar` ignora filtros en blanco; género y director se comparan exactos (la colación de MariaDB suele ignorar mayúsculas). Maven: `/opt/homebrew/Cellar/apache-maven-3.9.6/bin/mvn` (no está en el PATH).
- 6.2: `ValidacionException` (en `compartido`, 400) para reglas de negocio incumplidas; `ValoracionService.valorar` valida el rango antes de comprobar la película. Dobles: `PeliculaApiFalsa` y `ValoracionRepositoryFalso` en `src/test/.../valoracion/application`.
- 6.4: la BD de pruebas H2 se comparte entre tests de integración; los que crean películas deben borrarlas al terminar (ver `ValoracionControllerIntegracionTest`) o romperán el listado de `PeliculaControllerIntegracionTest`. `PUT /api/peliculas/{id}/valoracion` ya queda protegido por `anyRequest().authenticated()`.
- 7.2: `MarcaPeliculaService` (separado de `ValoracionService`) devuelve ids de película en `peliculasMarcadas`; para 7.3, listar "mis favoritas/vistas" con datos de película exige ampliar `PeliculaApi` (p. ej. consulta por ids) porque `valoracion` no puede usar `PeliculaRepository`.
- 7.3: `PeliculaApi.resumenes(ids)` y el record `PeliculaResumen` (raíz de `pelicula`) permiten a `valoracion` listar películas sin tocar `pelicula.domain`; cualquier implementación nueva de `PeliculaApi` o doble debe implementarlo.
- 8.1 (decisión): borrado coordinado por evento. `PeliculaService.eliminar` (con `@Transactional`) publica `PeliculaEliminada(Long peliculaId)` (record en `org.example.pelicula`) con `ApplicationEventPublisher`; `valoracion` lo escucha con `@EventListener` síncrono (misma transacción) y borra valoraciones y marcas de esa película. Alternativa descartada: `pelicula` llamando a `ValoracionApi` (ciclo entre módulos, lo prohíbe ArchUnit). `design.md` actualizado.
