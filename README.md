# Peliculas

API REST para gestionar un catálogo de películas: búsqueda y filtros, valoraciones, listas de favoritas y vistas, carrito, compras y pedidos con reserva de stock. Incluye una interfaz web mínima (`index.html` + `app.js`) y Swagger UI.

## Tecnología

| Área | Elección |
|---|---|
| Lenguaje y build | Java 26, Maven |
| Framework | Spring Boot 4.1 (Web MVC, Validation) |
| Seguridad | Spring Security, JWT sin estado, contraseñas con BCrypt |
| Persistencia | JDBC con `JdbcClient` (sin JPA), MariaDB, migraciones Flyway |
| Mapeo | MapStruct |
| Contratos | OpenAPI (interfaces y DTO generados con openapi-generator) |
| Errores | `ProblemDetail` (RFC 9457) |
| Pruebas | JUnit 5, Mockito, Testcontainers (MariaDB), H2, ArchUnit, Spring Modulith |

## Arquitectura

Monolito modular: un paquete por módulo de negocio bajo `shop/`, y dentro de cada módulo tres capas.

```
shop/
├── catalog  rating  authentication  cart  purchase  orders   # módulos
└── shared                                                    # errores, seguridad y paginación
    └── <módulo>/
        ├── api/http        controller: contrato HTTP, DTO web (Request/Response) y mapper web
        ├── api/facade      API pública del módulo para los demás
        └── internal/
            ├── domain      servicios, modelos anémicos, XDto, mappers, excepciones y contratos
            ├── persistence repositorios JDBC y adaptadores hacia otros módulos
            └── infrastructure / application   implementaciones técnicas y fachada
```

- `controller → domain ← persistence`: el dominio declara las interfaces (`XRepository`) y `persistence` las implementa.
- Las capas se comunican con DTO; el modelo no sale del servicio. La lógica de negocio vive en los servicios.
- Los módulos solo se conocen por su fachada pública; `ArchitectureTest` y `ModulithTest` lo verifican en cada build.
- Criterio de revisión del modular: `.kiro/steering/structure.md`.

## Requisitos

- JDK 26 y Maven
- Docker (las pruebas de repositorio usan Testcontainers)
- MariaDB 11 para ejecutar la aplicación

## Configuración

Variables de entorno:

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `JWT_SECRETO` | sí | | Secreto de firma del token (suficientemente largo para HS256) |
| `DB_URL` | no | `jdbc:mariadb://localhost:3306/peliculas` | Conexión a la base de datos |
| `DB_USUARIO` | no | `peliculas` | Usuario de la base de datos |
| `DB_CONTRASENA` | no | vacío | Contraseña de la base de datos |
| `JWT_CADUCIDAD` | no | `PT1H` | Caducidad del token (duración ISO-8601) |
| `ADMIN_EMAIL`, `ADMIN_CONTRASENA` | no | vacío | Crean el administrador inicial si no existe (ambas o ninguna) |

El esquema se crea y actualiza con Flyway al arrancar (`src/main/resources/db/migration`).

## Ejecutar

```bash
export JWT_SECRETO='un-secreto-largo-de-al-menos-32-caracteres'
export DB_CONTRASENA='...'
mvn spring-boot:run
```

- Interfaz web: <http://localhost:8080/>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

## Pruebas

```bash
mvn test
```

Necesita Docker en marcha. Incluye las pruebas de arquitectura (ArchUnit) y de límites entre módulos (Modulith).

## API

Los contratos están en `src/main/resources/static/contratos/` y en las interfaces `*HttpApi` de cada módulo. Todas las rutas exigen `Authorization: Bearer <token>`, salvo las públicas.

| Recurso | Rutas | Acceso |
|---|---|---|
| Autenticación | `POST /api/auth/registro`, `POST /api/auth/login` | público |
| Catálogo | `GET /api/peliculas`, `GET /api/peliculas/{id}` | público |
| Catálogo (admin) | `POST`, `PUT`, `DELETE /api/peliculas[/{id}]` | rol ADMIN |
| Valoraciones | `PUT /api/peliculas/{id}/valoracion` | usuario |
| Favoritas y vistas | `PUT`/`DELETE /api/peliculas/{id}/favorita` y `/vista`, `GET /api/usuarios/me/favoritas` y `/vistas` | usuario |
| Carrito | `/api/carrito`, `/api/carrito/lineas`, `/api/carrito/lineas/{peliculaId}` | usuario |
| Compras | `/api/compras`, `/api/compras/{compraId}` | usuario |
| Pedidos | `POST /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` | usuario |

El usuario se deduce siempre del token. Un pedido o una compra solo los ve su propietario; si no lo es, la respuesta es 404. Los errores usan `application/problem+json`.

## Desarrollo guiado por especificaciones

El proyecto sigue un flujo Kiro (`.kiro/steering/`, `.kiro/specs/`) con skills en `.claude/skills/`. Para el código del issue: `/do-code impl <plan.md>` y `/do-code review`.
