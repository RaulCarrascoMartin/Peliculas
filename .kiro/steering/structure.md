# Estructura

Monolito modular: una única aplicación desplegable dividida en módulos por funcionalidad. Sin puertos ni adaptadores; dentro de cada módulo, el dominio no depende de nada del módulo y el resto de capas dependen de él.

```
src/main/java/shop/
├── ShopApplication.java          # punto de entrada (Spring Boot)
├── <modulo>/                     # catalog, orders, valoracion, autenticacion, carrito, compra
│   ├── api/                      # parte pública: lo único que ven otros módulos
│   │   └── XxxApi y records públicos (p. ej. CatalogApi, PeliculaResumen, CarritoApi)
│   └── internal/                 # parte interna: nadie fuera del módulo la usa
│       ├── XxxApiImpl            # record que implementa la Api; único punto de salida hacia otros módulos
│       ├── 
│       ├── domain/               # entidades y repositorios (Spring Data `JpaRepository`)
│       ├── usecase/              # un caso de uso por `record` con `ejecutar(...)`, más los records de entrada/salida (p. ej. CrearPelicula, FinalizarCompra, CompraVista)
│       └── web/                  # controladores REST y DTOs (o interfaces generadas desde el contrato OpenAPI)
└── compartido/                   # utilidades comunes; no depende de ningún módulo
    ├── error/                    # RecursoNoEncontradoException, ConflictoException, ValidacionException, ErrorRespuesta, GlobalExceptionHandler
    └── seguridad/                # SecurityConfig, JwtFilter, JwtTokens, DatosToken, UsuarioActual
src/test/java/shop/        # mismas rutas que main, más pruebas de arquitectura
.kiro/specs/                      # specs (requirements, design, tasks)
```

## Módulo orders (P6)
- `orders` usa persistencia JDBC (`JdbcClient`) en lugar de JPA: `internal/domain` tiene `Order`, `OrderLine` y la interfaz `OrderRepository`; `internal/persistence` tiene `JdbcOrderRepository`. Sus tablas (`orders`, `order_lines`) están en `src/main/resources/init.sql`.
- Solo consume `shop.catalog.api.CatalogApi` (`reserve` y `release`); traduce sus excepciones a 404/409 en el caso de uso.
- Contrato: `static/contratos/orders-api.yml` (v0.2.0), del que se genera `OrdersRestApi` y los DTO.

## Reglas de dependencias
- Dentro de `internal`: `web` → `usecase` → `domain`, y `web` → `domain`. `domain` no depende de `usecase` ni de `web`.
- `domain` puede usar Spring y JPA (anotaciones, Spring Data); el framework no va a cambiar.
- No hay puertos: los casos de uso (`usecase`) usan directamente los repositorios del dominio.
- Los repositorios extienden `JpaRepository<Entidad, Id>`.
- Entre módulos: un módulo solo accede al paquete `api` de otro y a nada más; todo `internal` es privado del módulo. `api` no depende de `internal`.
- Una `Api` puede devolver o publicar tipos públicos de `api` (por ejemplo `PeliculaResumen`), nunca entidades.
- No hay comunicación por eventos entre módulos: solo llamadas a la `Api` del otro módulo. Al borrar una película no se borran en cascada sus valoraciones, marcas ni líneas de carrito; las lecturas ignoran las películas inexistentes.
- No se permiten dependencias cíclicas entre módulos.
- Spring Modulith (`ModulithTest`) verifica los límites en cada build: `api` de cada módulo es `@NamedInterface("api")` y `compartido` es un módulo abierto.
- `compartido` no depende de ningún módulo.

## Casos de uso y transporte de datos
- Cada caso de uso es una clase `@Service` en `internal/usecase` con un método `ejecutar(...)`; la transacción se declara con `@Transactional` donde hace falta.
- Los datos se transportan siempre con DTOs, que son `record` sin anotaciones de Spring: `PeliculaResumen` (en `api`) entre módulos, y `CompraVista`, `ValoracionVista`, `DatosPelicula`... entre `usecase` y `web`.
- Los casos de uso reciben y devuelven DTOs. Las entidades no salen de `domain` ni de `usecase`, y `web` no las ve.

## Modelo anémico y DTOs
- Las entidades de `domain` son anémicas: solo datos (campos, getters y setters, anotaciones JPA). La lógica de negocio vive en los servicios de `usecase`.
- Los controladores nunca reciben ni devuelven entidades: reciben `XxxRequest` y devuelven `XxxResponse`.
- `Request` y `Response` son `record` en `web`, y el mapeo entre DTO y entidad se hace en `web`. Los `Request` llevan las validaciones de entrada (Bean Validation).
- `usecase` trabaja con entidades y no conoce los DTO.

## Manejo de errores
- Los servicios lanzan excepciones de negocio (por ejemplo `RecursoNoEncontradoException`, `ConflictoException`, `ValidacionException`) en lugar de devolver códigos.
- Los controladores no capturan excepciones; `GlobalExceptionHandler` (en `compartido/error`) las traduce a un cuerpo JSON uniforme y al estado HTTP correspondiente.
- Las excepciones de negocio viven en `compartido/error`, para que todos los módulos las usen sin depender unos de otros.

## Autenticación con JWT
- API sin estado: no hay sesión HTTP. El cliente envía `Authorization: Bearer <token>` en cada petición.
- El módulo `autenticacion` registra usuarios, comprueba credenciales (contraseña con BCrypt) y emite el token.
- El token lleva como claims el id del usuario y su rol, de modo que `JwtFilter` (en `compartido/seguridad`) lo valida y rellena el `SecurityContext` sin consultar la base de datos ni depender de ningún módulo.
- Rutas públicas: registro, login y consulta del catálogo. El resto exige token; crear, editar y eliminar películas exige el rol administrador.
- El secreto de firma y la caducidad del token se leen de la configuración, nunca del código.

## Usuario autenticado (SecurityContext)
- Spring Security guarda al usuario autenticado en `SecurityContextHolder`, que por defecto usa `ThreadLocal`: el contexto es el del hilo que atiende la petición.
- Ningún código lee `SecurityContextHolder` directamente, salvo `compartido/seguridad/UsuarioActual`, que expone el id y los roles del usuario actual.
- `usecase` y `web` obtienen el usuario mediante `UsuarioActual`; las operaciones de administrador se protegen con roles (`@PreAuthorize`).
- El `ThreadLocal` no se propaga a hilos nuevos (`@Async`, `CompletableFuture`, executors propios): si hace falta, se pasa el usuario como parámetro o se usa `DelegatingSecurityContextExecutor`.
- En pruebas unitarias no se usa `SecurityContextHolder`: se escribe un doble a mano de `UsuarioActual`.

## Pruebas
- Las pruebas de ArchUnit viven en `src/test/java/shop/arquitectura/` y validan estas reglas.
- Las pruebas de `domain` y `usecase` son unitarias, sin levantar el contexto de Spring; usan dobles escritos a mano.
