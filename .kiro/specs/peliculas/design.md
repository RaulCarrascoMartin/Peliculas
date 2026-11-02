# Diseño

## Visión general
Monolito modular en Spring Boot que expone una API REST con JSON, sin estado y autenticada con JWT. Cada módulo (`pelicula`, `valoracion`, `autenticacion`) tiene las capas `domain`, `application` y `web`. Las reglas de dependencias y la estructura de paquetes están en `.kiro/steering/structure.md`.

## Arquitectura

```
Cliente ──► JwtFilter ──► Controller (web) ──► Service (application) ──► Repository (domain) ──► MariaDB
                │               │                      │
                │          Request/Response       entidades anémicas
                └─► SecurityContext (ThreadLocal) ◄── UsuarioActual
```

- `JwtFilter` valida el token y rellena el `SecurityContext` con id y rol.
- Los controladores convierten `Request` a entidad, llaman al servicio y convierten el resultado a `Response`.
- Los servicios contienen la lógica de negocio y usan directamente los `JpaRepository` del dominio.
- Los módulos se comunican solo por su clase `Api`.

## Componentes e interfaces

### Módulo `autenticacion`
- `AutenticacionService`: registra usuarios (email único, contraseña con BCrypt), comprueba credenciales y emite el JWT.
- `AutenticacionController`: endpoints de registro y login.

### Módulo `pelicula`
- `PeliculaService` (implementa `PeliculaApi`): alta, edición, baja, detalle, listado paginado y búsqueda con filtros. `PeliculaApi` expone `existe(id)`, la actualización de la valoración media y `resumenes(ids)`, que devuelve los datos públicos (`PeliculaResumen`) de varias películas. Al eliminar una película publica el evento `PeliculaEliminada`.
- `PeliculaRepository`: `JpaRepository<Pelicula, Long>` con consultas por título (sin distinguir mayúsculas), género, año y director.
- `PeliculaController`.

### Módulo `valoracion`
- `ValoracionService`: crea o actualiza la valoración del usuario actual (puntuación de 1 a 5) y recalcula la media.
- `MarcaPeliculaService`: marca, desmarca y lista las películas favoritas y vistas del usuario actual.
- `LimpiezaPorPeliculaEliminada`: oyente de `PeliculaEliminada` que borra las valoraciones y marcas de la película.
- Usan `PeliculaApi` para comprobar que la película existe, guardar la media y obtener los datos de las listas, y `UsuarioActual` para saber quién actúa. El módulo no expone ninguna `Api` propia porque ningún módulo la necesita.
- `ValoracionController` y `MarcaPeliculaController`.

### `compartido`
- `SecurityConfig`: sin sesión, rutas públicas y protegidas, `JwtFilter` en la cadena.
- `JwtFilter`: lee `Authorization: Bearer`, valida firma y caducidad, y rellena el `SecurityContext`.
- `UsuarioActual`: único punto de lectura de `SecurityContextHolder`.
- `GlobalExceptionHandler`: traduce excepciones a respuestas de error.

## API REST

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | público | Registro de usuario |
| POST | `/api/auth/login` | público | Devuelve el token JWT |
| GET | `/api/peliculas` | público | Listado paginado; filtros `titulo`, `genero`, `anio`, `director` |
| GET | `/api/peliculas/{id}` | público | Detalle con valoración media |
| POST | `/api/peliculas` | administrador | Crear película |
| PUT | `/api/peliculas/{id}` | administrador | Editar película |
| DELETE | `/api/peliculas/{id}` | administrador | Eliminar película y sus valoraciones |
| PUT | `/api/peliculas/{id}/valoracion` | usuario | Crear o actualizar mi valoración |
| PUT | `/api/peliculas/{id}/favorita` | usuario | Marcar como favorita |
| DELETE | `/api/peliculas/{id}/favorita` | usuario | Quitar de favoritas |
| PUT | `/api/peliculas/{id}/vista` | usuario | Marcar como vista |
| DELETE | `/api/peliculas/{id}/vista` | usuario | Quitar de vistas |
| GET | `/api/usuarios/me/favoritas` | usuario | Mis favoritas |
| GET | `/api/usuarios/me/vistas` | usuario | Mis vistas |

## Modelo de datos
Las entidades son anémicas (datos y anotaciones JPA, sin lógica).

| Entidad | Campos | Restricciones |
|---|---|---|
| `Usuario` | id, email, contrasena (hash), rol (`USUARIO`/`ADMIN`) | email único |
| `Pelicula` | id, titulo, anio, genero, director, valoracionMedia | titulo obligatorio |
| `Valoracion` | id, usuarioId, peliculaId, puntuacion (1-5), comentario | única por (usuarioId, peliculaId) |
| `MarcaPelicula` | id, usuarioId, peliculaId, tipo (`FAVORITA`/`VISTA`) | única por (usuarioId, peliculaId, tipo) |

- Las relaciones entre módulos se guardan como ids, no como asociaciones JPA, para no acoplar los módulos.
- Al borrar una película se eliminan sus valoraciones y marcas. `pelicula` publica un evento `PeliculaEliminada` (record en la raíz de `pelicula`, parte de su API pública) dentro de la transacción del borrado y `valoracion` lo escucha con `@EventListener`. Así `pelicula` no depende de `valoracion` y no hay ciclo entre módulos (`valoracion` ya depende de `pelicula` por `PeliculaApi`).

### DTOs
- `PeliculaRequest`, `PeliculaResponse`, `ValoracionRequest`, `ValoracionResponse`, `PeliculaMarcadaResponse`, `RegistroRequest`, `LoginRequest`, `TokenResponse`: todos `record` en la capa `web` de su módulo.

## Autenticación con JWT
1. El usuario envía email y contraseña a `/api/auth/login`.
2. `AutenticacionService` comprueba la contraseña con BCrypt y firma un JWT con claims `sub` (id), `rol` y `exp`.
3. El cliente envía el token en cada petición protegida.
4. `JwtFilter` valida firma y caducidad y rellena el `SecurityContext`; no consulta la base de datos.
5. Los servicios obtienen el usuario con `UsuarioActual`; los endpoints de administrador usan `@PreAuthorize("hasRole('ADMIN')")`.

El secreto y la caducidad vienen de la configuración. No hay refresh token. El primer administrador se crea al arrancar a partir de la configuración.

## Manejo de errores
`GlobalExceptionHandler` devuelve un cuerpo JSON uniforme (`estado`, `mensaje`, y `errores` por campo en validación).

| Situación | Estado |
|---|---|
| Validación de `Request` fallida o regla de negocio incumplida (`ValidacionException`, p. ej. puntuación fuera de 1-5) | 400 |
| Credenciales incorrectas, token ausente, inválido o caducado | 401 |
| Rol insuficiente | 403 |
| Película o recurso inexistente | 404 |
| Email ya registrado | 409 |

## Estrategia de pruebas
- **Unitarias de `application`:** JUnit 5 con dobles escritos a mano. `UsuarioActual` y `PeliculaApi` tienen dobles propios; los `JpaRepository` también, y como ese interfaz es grande, se mantiene una clase base de doble en memoria por repositorio.
- **Arquitectura (ArchUnit):** `domain` no depende de `application` ni `web`; los módulos solo se acceden por su `Api`; sin ciclos entre módulos; `compartido` no depende de módulos.
- **Integración:** pruebas de controladores con seguridad real contra una base de datos de prueba, cubriendo los códigos de la tabla de errores y los accesos por rol.
- **Seguridad:** token ausente, caducado, con firma alterada y con rol insuficiente.
