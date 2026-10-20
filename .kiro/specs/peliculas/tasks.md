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
  - [ ] 2.4 Pruebas del manejo de errores
  - _Requisitos: 0.4, 0.5, 1.5, 1.6_

- [ ] 3. Módulo `autenticacion`
  - [ ] 3.1 Entidad anémica `Usuario` y `UsuarioRepository`
  - [ ] 3.2 `AutenticacionService`: registro con BCrypt, rechazo de email duplicado, login y emisión del JWT
  - [ ] 3.3 `RegistroRequest`, `LoginRequest`, `TokenResponse` y `AutenticacionController`
  - [ ] 3.4 `JwtFilter` y `SecurityConfig` (sin sesión, rutas públicas y protegidas, `@PreAuthorize` por rol)
  - [ ] 3.5 Crear el primer administrador al arrancar a partir de la configuración
  - [ ] 3.6 Pruebas unitarias del servicio y de integración de seguridad (sin token, caducado, firma alterada, rol insuficiente)
  - _Requisitos: 0.1, 0.2, 0.3, 0.4, 0.5_

- [ ] 4. Módulo `pelicula`: catálogo
  - [ ] 4.1 Entidad anémica `Pelicula` y `PeliculaRepository`
  - [ ] 4.2 `PeliculaService`: alta, edición, baja y detalle, con error 404 si no existe
  - [ ] 4.3 `PeliculaRequest` (con validaciones), `PeliculaResponse` y `PeliculaController`, con creación, edición y baja solo para administrador
  - [ ] 4.4 `PeliculaApi` con `existe(id)` y actualización de la valoración media
  - [ ] 4.5 Pruebas unitarias del servicio e integración del controlador
  - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2_

- [ ] 5. Módulo `pelicula`: consulta, búsqueda y filtros
  - [ ] 5.1 Listado paginado
  - [ ] 5.2 Búsqueda por título sin distinguir mayúsculas y filtros por género, año y director combinables
  - [ ] 5.3 Lista vacía cuando no hay resultados
  - [ ] 5.4 Pruebas del servicio y del controlador
  - _Requisitos: 2.1, 3.1, 3.2, 3.3_

- [ ] 6. Módulo `valoracion`: valoraciones
  - [ ] 6.1 Entidad anémica `Valoracion`, restricción única (usuario, película) y `ValoracionRepository`
  - [ ] 6.2 `ValoracionService`: crear o actualizar la valoración del usuario actual, validar el rango 1-5 y comprobar que la película existe con `PeliculaApi`
  - [ ] 6.3 Recalcular la media y guardarla mediante `PeliculaApi`
  - [ ] 6.4 `ValoracionRequest`, `ValoracionResponse` y `ValoracionController`
  - [ ] 6.5 Pruebas con dobles escritos a mano de `PeliculaApi` y `UsuarioActual`
  - _Requisitos: 4.1, 4.2, 4.3, 4.4_

- [ ] 7. Módulo `valoracion`: favoritas y vistas
  - [ ] 7.1 Entidad anémica `MarcaPelicula` y su repositorio
  - [ ] 7.2 Servicio para marcar y desmarcar películas como favoritas o vistas
  - [ ] 7.3 Endpoints para marcar, desmarcar y consultar las propias listas
  - [ ] 7.4 Pruebas del servicio y del controlador
  - _Requisitos: 5.1, 5.2, 5.3_

- [ ] 8. Borrado coordinado entre módulos
  - [ ] 8.1 Decidir cómo `valoracion` limpia sus datos al borrar una película sin crear un ciclo entre módulos (propuesta: `pelicula` publica un evento `PeliculaEliminada` y `valoracion` lo escucha)
  - [ ] 8.2 Implementar el borrado de valoraciones y marcas
  - [ ] 8.3 Prueba de integración del borrado
  - _Requisitos: 1.4_

- [ ] 9. Cierre
  - [ ] 9.1 Revisar la cobertura de requisitos con las pruebas de integración
  - [ ] 9.2 Ejecutar `mvn test` completo, incluidas las pruebas de ArchUnit
