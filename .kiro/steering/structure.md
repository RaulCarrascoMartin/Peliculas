# Estructura

Monolito modular: una única aplicación desplegable dividida en módulos por funcionalidad. Sin puertos ni adaptadores; dentro de cada módulo, el dominio no depende de nada del módulo y el resto de capas dependen de él.

```
src/main/java/shop/
├── ShopApplication.java          # punto de entrada (Spring Boot)
├── app/                          # raíz de composición: @Configuration por módulo que cablea las implementaciones; sin lógica
├── <modulo>/                     # catalog, rating, authentication, cart, purchase, orders
│   ├── package-info.java         # @ApplicationModule: nombre y allowedDependencies (p. ej. "catalog::facade")
│   ├── api/
│   │   ├── facade/               # API pública entre módulos (@NamedInterface("facade") también en sus subpaquetes)
│   │   │   ├── XxxFacade         #   interfaz
│   │   │   ├── dto/              #   records del contrato (*Dto)
│   │   │   └── exception/        #   excepciones del contrato
│   │   └── http/                 # contrato HTTP (XxxHttpApi generado del OpenAPI)
│   │       ├── controller/       #   controladores
│   │       ├── dto/              #   DTO web (generados o *Request/*Response)
│   │       └── mapper/           #   *WebMapper: vista de caso de uso -> DTO
│   └── internal/                 # privado del módulo
│       ├── application/
│       │   ├── usecase/          #   casos de uso (`execute(...)`)
│       │   ├── dto/              #   *View, *Data y entradas de los casos de uso (records)
│       │   ├── mapper/           #   modelo -> vista
│       │   └── facade/           #   XxxFacadeImpl
│       ├── domain/
│       │   ├── model/            #   POJOs, enums y value objects, sin frameworks
│       │   ├── service/          #   XxxService + XxxServiceImpl (las reglas de negocio)
│       │   ├── repository/       #   interfaces de repositorio
│       │   ├── contract/         #   lo que el módulo necesita de otros (MovieCatalog, UserCart, StockReservation…)
│       │   └── exception/        #   excepciones de negocio
│       ├── infrastructure/       # piezas técnicas: hash de contraseñas, emisión de tokens
│       └── persistence/
│           ├── entity/           #   XxxEntity (JPA)
│           ├── jpa/              #   XxxJpaRepository (Spring Data)
│           ├── repository/       #   XxxRepositoryImpl (implementa el dominio)
│           ├── mapper/           #   XxxPersistenceMapper
│           └── adapter/          #   adaptadores hacia la facade de otros módulos
└── shared/                       # utilidades comunes (módulo abierto); no depende de ningún módulo
    ├── error/                    # excepciones de negocio, ApiError, GlobalExceptionHandler
    ├── pagination/               # PageQuery y PageResult (paginación sin Spring)
    └── security/                 # SecurityConfig, JwtFilter, JwtTokens, TokenData, CurrentUser
src/test/java/shop/               # mismas rutas que main, más pruebas de arquitectura
.kiro/specs/                      # specs (requirements, design, tasks)
```

## Reglas de la estructura modular
- Las reglas de negocio viven en los servicios de `domain`; los casos de uso de `application` solo coordinan (transacción, mapeo a `*View`) y reciben el `userId` como parámetro (el controlador lo obtiene de `CurrentUser`).
- `domain` y `application` no dependen de Spring, JPA, validación, Hibernate ni MapStruct. Las clases internas son POJOs con constructor; solo `app` (y `api.http`, `persistence`, `infrastructure`) usan el framework. Las transacciones se declaran con `jakarta.transaction.Transactional`.
- Los beans de `application`, `persistence` e `infrastructure` se crean en `shop.app` con `@Bean`; los controladores (`api.http`) se detectan por `@RestController`.
- El módulo consumidor define su propio contrato en `domain` (p. ej. `MovieCatalog`) y un adaptador en `persistence` lo traduce a la `facade` del proveedor. Los casos de uso nunca ven tipos de otro módulo.
- `api.http` solo ve `application` (y enums de dominio usados como parámetro); nunca modelos ni entidades.
- Spring Modulith usa `spring.modulith.detection-strategy=explicitly-annotated`: solo los paquetes con `@ApplicationModule` son módulos (`shop.app` no lo es y es el único que ve el `internal` de todos).
- `ArchitectureTest` y `ModulithTest` verifican estas reglas en cada build.

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
