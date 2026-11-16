# Estructura

Monolito modular: una única aplicación desplegable dividida en módulos por funcionalidad (Spring Modulith). Dentro de cada módulo se aplican las reglas de backend del vault (skills `k-back-project`, `k-back-controller`, `k-back-service`, `k-back-repository`): tres capas **controller → domain ← persistence**, comunicación entre capas con DTO y un subpaquete por tipo de clase.

> Excepción decidida a las skills: dicen «sin módulos», pero este proyecto conserva los módulos, sus facades y Modulith. Donde las skills hablan de los paquetes `controller` / `domain` / `persistence` / `config`, aquí son `api/http` / `internal/domain` / `internal/persistence` / `internal/infrastructure` de cada módulo.

```
src/main/java/shop/
├── ShopApplication.java          # punto de entrada (Spring Boot); los beans se detectan por component scan
├── <modulo>/                     # catalog, rating, authentication, cart, purchase, orders
│   ├── package-info.java         # @ApplicationModule: nombre y allowedDependencies (p. ej. "catalog::facade")
│   ├── api/
│   │   ├── facade/               # API pública entre módulos (@NamedInterface("facade") también en sus subpaquetes)
│   │   │   ├── XxxFacade         #   interfaz
│   │   │   ├── dto/              #   records del contrato (*Dto)
│   │   │   └── exception/        #   excepciones del contrato
│   │   └── http/                 # capa controller
│   │       ├── XxxHttpApi        #   contrato HTTP: generado del OpenAPI o interfaz a mano con @Operation/@ApiResponses
│   │       ├── controller/       #   XxxController implements XxxHttpApi: solo delega en el servicio
│   │       ├── dto/              #   DTO web: records *Request / *Response con validación Jakarta
│   │       └── mapper/           #   XxxWebMapper (MapStruct): DTO web <-> DTO de dominio
│   └── internal/                 # privado del módulo
│       ├── domain/               # capa domain
│       │   ├── model/            #   modelos anémicos y enums; no salen de service + mapper
│       │   ├── dto/              #   XxxDto (records): lo que reciben y devuelven servicio y repositorio
│       │   ├── mapper/           #   XxxMapper (MapStruct): DTO <-> modelo
│       │   ├── service/          #   XxxService + XxxServiceImpl (@Service): todas las reglas de negocio
│       │   ├── repository/       #   interfaces XxxRepository (con DTO)
│       │   ├── contract/         #   lo que el módulo necesita de otros (MovieCatalog, UserCart, StockReservation…)
│       │   └── exception/        #   una excepción por situación de negocio
│       ├── persistence/          # capa persistence
│       │   ├── repository/       #   XxxRepositoryImpl (@Named, JdbcClient, RowMapper privado -> DTO)
│       │   └── adapter/          #   adaptadores de `contract` hacia la facade de otros módulos (@Named)
│       ├── infrastructure/       # implementaciones técnicas de interfaces del dominio: hash, tokens, arranque
│       └── application/facade/   # solo XxxFacadeImpl (@Component), si el módulo publica facade; llama a los servicios
└── shared/                       # utilidades comunes (módulo abierto); no depende de ningún módulo
    ├── error/                    # categorías de excepción y GlobalExceptionHandler (ProblemDetail)
    ├── pagination/               # PageQuery y PageResult
    └── security/                 # SecurityConfig, JwtFilter, JwtTokens, CurrentUser, 401/403 como ProblemDetail
src/main/resources/db/migration/  # migraciones Flyway (V1__init.sql…)
src/test/java/shop/               # mismas rutas que main, más pruebas de arquitectura
.kiro/specs/                      # specs (requirements, design, tasks)
```

## Capas dentro de un módulo
- **Controller (`api.http`)**: frontera HTTP. Convierte el DTO web al DTO de dominio con el `XxxWebMapper`, llama a la interfaz `XxxService` y responde. Sin lógica de negocio, sin repositorios, sin `try/catch`. Validación de formato con Jakarta (`@Valid`); autorización con `@PreAuthorize`. El usuario autenticado se obtiene de `CurrentUser` y se pasa al servicio como parámetro.
- **Domain (`internal.domain`)**: el servicio contiene todas las reglas, recibe y devuelve `XxxDto` y usa el modelo anémico solo internamente (DTO → `mapper.toModel` → reglas → `mapper.toDto`). Declara las interfaces de repositorio y los `contract`. No conoce HTTP, SQL ni validación de formato. Puede usar Spring (`@Service`, `@Transactional`) y MapStruct.
- **Persistence (`internal.persistence`)**: implementa las interfaces del dominio con SQL explícito sobre `JdbcClient` (parámetros con nombre, sin `select *`, hijos sin N+1). Un repositorio por agregado: las tablas hijas las gestiona el repositorio del padre. Sin reglas de negocio; traduce las violaciones de unicidad a la excepción de dominio.
- No hay capa de casos de uso: el controller habla directamente con el servicio.
- Inyección por constructor con campos `private final` en todas las capas; los beans se registran con `@Service`, `@Named`, `@Component` y `@Mapper(componentModel = "spring")`. Los nombres simples de servicios, mappers y adaptadores son únicos en toda la aplicación (o llevan nombre explícito, p. ej. `@Named("cartMovieCatalogAdapter")`).

## Transacciones
- `@Transactional` de Spring en los métodos del servicio que modifican datos como unidad de trabajo; `readOnly = true` en las lecturas. El repositorio la hereda.
- No se añade transacción donde el diseño es deliberadamente no transaccional: la reserva y liberación de stock (`catalog`) y la creación y cancelación de pedidos (`orders`) se apoyan en escrituras condicionales con compensación, porque cruzan la facade de otro módulo.
- El bloqueo optimista del carrito (`carritos.version`) se hace a mano en su repositorio y depende de aislamiento `REPEATABLE READ` (`spring.datasource.hikari.transaction-isolation`). Los fallos de concurrencia se traducen a 409 en `GlobalExceptionHandler`.

## Reglas entre módulos
- Un módulo solo accede a la `api.facade` de otro; todo `internal` y `api.http` son privados. La facade no depende de su `internal`.
- El módulo consumidor define su propio contrato en `domain/contract` (p. ej. `MovieCatalog`) y un adaptador en `persistence/adapter` lo traduce a la facade del proveedor, incluidas sus excepciones. El servicio nunca ve tipos de otro módulo.
- No hay comunicación por eventos entre módulos ni claves foráneas entre tablas de módulos distintos. Al borrar una película no se borran en cascada sus valoraciones, marcas ni líneas de carrito; las lecturas ignoran las películas inexistentes.
- Sin dependencias cíclicas entre módulos; `shared` no depende de ningún módulo.
- Spring Modulith usa `spring.modulith.detection-strategy=explicitly-annotated`: solo los paquetes con `@ApplicationModule` son módulos.

## Persistencia y esquema
- JDBC (`JdbcClient`) en todos los módulos, sin JPA ni ORM.
- El esquema lo crean las migraciones Flyway de `src/main/resources/db/migration`; la aplicación no lo crea ni lo modifica por su cuenta. Un cambio de esquema es una migración nueva (`V2__...`), nunca una edición de una ya aplicada. Las bases creadas antes de Flyway se marcan en la versión 1 (`baseline-on-migrate`).

## Contratos HTTP (API First)
- Una única fuente de verdad por controller: el `yml` de `static/contratos/` (cart, purchase, orders), del que se generan `XxxHttpApi` y los DTO, o una interfaz `XxxHttpApi` escrita a mano y documentada con anotaciones OpenAPI.
- Los cambios de contrato deben ser compatibles con los consumidores (el front de `static/app.js` y los clientes de la API).

## Manejo de errores
- Los servicios lanzan excepciones de dominio con nombre por situación (`OrderNotFoundException`, `MovieAlreadyInCartException`…), sin códigos HTTP. Cada una extiende una categoría de `shared/error` (`ResourceNotFoundException` → 404, `ConflictException` → 409, `BusinessValidationException` → 400, `InvalidCredentialsException` → 401); nunca se lanza la categoría directamente.
- Los controladores no capturan excepciones; `GlobalExceptionHandler` (en `shared/error`, porque sirve a todos los módulos y no puede ver su `internal`) las traduce por categoría.
- Formato único: `ProblemDetail` (RFC 9457, `application/problem+json`). En errores de validación añade la propiedad `errors`, con una entrada `"campo: mensaje"` por campo rechazado. Los 401/403 de la cadena de seguridad salen con el mismo formato. Sin detalles internos en la respuesta.

## Autenticación con JWT
- API sin estado: no hay sesión HTTP. El cliente envía `Authorization: Bearer <token>` en cada petición.
- El módulo `authentication` registra usuarios, comprueba credenciales (contraseña con BCrypt) y emite el token.
- El token lleva como claims el id del usuario y su rol, de modo que `JwtFilter` (en `shared/security`) lo valida y rellena el `SecurityContext` sin consultar la base de datos ni depender de ningún módulo.
- Rutas públicas: registro, login y consulta del catálogo. El resto exige token; crear, editar y eliminar películas exige el rol administrador.
- El secreto de firma y la caducidad del token se leen de la configuración, nunca del código.

## Usuario autenticado (SecurityContext)
- Spring Security guarda al usuario autenticado en `SecurityContextHolder`, que por defecto usa `ThreadLocal`: el contexto es el del hilo que atiende la petición.
- Ningún código lee `SecurityContextHolder` directamente, salvo `shared/security/CurrentUser`, que expone el id y los roles del usuario actual.
- Los controladores obtienen el usuario mediante `CurrentUser` y pasan su id al servicio; las operaciones de administrador se protegen con roles (`@PreAuthorize`).
- El `ThreadLocal` no se propaga a hilos nuevos (`@Async`, `CompletableFuture`, executors propios): si hace falta, se pasa el usuario como parámetro o se usa `DelegatingSecurityContextExecutor`.
- En pruebas unitarias no se usa `SecurityContextHolder`: se pasa el id del usuario como parámetro al servicio.

## Pruebas
- Toda clase que se crea o modifica lleva su prueba.
- Servicio: unitarias con mocks (Mockito) de repositorios y contratos y el mapper MapStruct real; sin Spring ni base de datos.
- Mappers (dominio y web): unitarias de ida y vuelta.
- Repositorio: `@JdbcTest` contra MariaDB real con Testcontainers (extienden `shop.shared.MariaDbRepositoryTest`; requieren Docker).
- Controller: `@WebMvcTest` con el servicio como `@MockitoBean` y la `SecurityConfig` real: códigos, cuerpos, 400 con el campo, traducción de errores, 401/403.
- Integración (`@SpringBootTest`, sobre H2 en modo MariaDB) y E2E para los flujos completos. Una prueba de un módulo no importa el `internal` de otro.
- `ArchitectureTest` (ArchUnit) y `ModulithTest` verifican en cada build las dependencias entre capas, la ubicación de cada tipo de clase y los límites entre módulos.

## Criterio de revisión: monolito modular
- El proyecto es un **monolito modular** (`shop/<módulo>/api|internal`), no el monolito por capas puro de las skills `k-back-*`, que dicen "no uses módulos". Las skills se aplican **dentro de cada módulo**, con las excepciones de esta lista, que `ArchitectureTest` y `ModulithTest` ya vigilan.
- Se aplican siempre, también en un modular: transacciones en los servicios que escriben, el usuario sale del token (`CurrentUser`) y nunca de la petición, un recurso solo lo ve su propietario, escrituras condicionales que comprueban las filas afectadas, y toda clase con su prueba.
- **No se marcan como hallazgo** (decisiones del modular):
  - Las fachadas entre módulos (`api/facade`, `internal/application/facade`) y la traducción de excepciones en la frontera de un módulo.
  - `internal/infrastructure` en vez de `config`, y `shop/shared` para lo transversal.
  - Los enums que viajan en los DTO pueden vivir en `internal/domain/model`: `ArchitectureTest` los excluye de "el modelo no sale del servicio".
  - Adaptadores con el mismo nombre en módulos distintos (`MovieCatalogAdapter`), con nombre de bean explícito.
  - Preferencias de estilo de las skills (filtros sueltos en vez de un `XDto`, parseo del `sort` en el mapper web).
- Quien revise (persona o `/do-code review`) debe leer esta sección antes y no repetir esos hallazgos.
