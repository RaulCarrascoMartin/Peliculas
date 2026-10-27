# Plan de implementación

- [ ] 1. Preparar la generación de código y la UI del contrato
  - [x] 1.1 Spike: añadir al `pom.xml` el plugin `openapi-generator-maven-plugin` 7.26.0 (dos ejecuciones, una por módulo, apuntando a `static/contratos/carrito-api.yml` y `compras-api.yml`) con las opciones `useSpringBoot4`, `useJackson3`, `useRecords`, `interfaceOnly`, `skipDefaultInterface`, `useTags`, `requestMappingMode=api_interface`, `useResponseEntity=false`, `openApiNullable=false`, `documentationProvider=none`, `annotationLibrary=none`, `useSwaggerUI=false`, `dateLibrary=java8`, `hideGenerationTimestamp=true`; añadir el webjar `org.webjars:swagger-ui`; crear los dos YAML mínimos temporales para validar el build
    - Ejecutar `mvn generate-sources` y `mvn compile`: el código generado compila con Boot 4.1 / Java 26
    - Verificar que Spring MVC registra los mapeos heredados de las interfaces generadas (arranque o prueba MockMvc)
    - Si Jackson 3 diera problemas, plan B: Jackson 2 en Boot 4, documentado en notas
    - Observable: build verde con la generación activa y el mojo `validate` de los YAML en el build
    - _Boundary: build/codegen_
  - [x] 1.2 Crear `src/main/resources/static/swagger-ui.html` (SwaggerUIBundle con `urls` a `/contratos/carrito-api.yml` y `/contratos/compras-api.yml`, `persistAuthorization: true`) y añadir `permitAll` en `SecurityConfig` para `GET /swagger-ui.html`, `/webjars/**` y `/contratos/**`; extender `FrontEstaticoIntegracionTest`
    - Observable: sin token se cargan la página, los YAML y los recursos del webjar; con token el Try it out envía `Authorization`
    - _Boundary: estático/compartido_
    - _Depends: 1.1_
  - _Requisitos: 8.2, 8.3_

- [ ] 2. Contratos OpenAPI definitivos
  - [x] 2.1 (P) Escribir `static/contratos/carrito-api.yml`: paths `/api/carrito`, `/api/carrito/lineas`, `/api/carrito/lineas/{peliculaId}`; schemas `Carrito`, `LineaCarrito`, `AnadirLinea`, `ErrorRespuesta`; `bearerAuth` global; ejemplos por error
    - Observable: el YAML valida (mojo validate) y al regenerar aparecen `CarritoRestApi` y los DTO en `org.example.carrito.web.api/.dto`
    - _Boundary: contrato carrito_
    - _Depends: 1.1_
  - [x] 2.2 (P) Escribir `static/contratos/compras-api.yml`: paths `/api/compras`, `/api/compras/{compraId}`; schemas `Compra` (fecha `date-time`, estado enum `COMPRADA`, lineas, total), `LineaCompra`, `ErrorRespuesta` idéntico al de carrito; `bearerAuth`; ejemplos
    - Observable: al regenerar aparecen `ComprasRestApi` y los DTO en `org.example.compra.web.api/.dto`
    - _Boundary: contrato compra_
    - _Depends: 1.1_
  - [x] 2.3 Regenerar todo, compilar y verificar la servida estática de `/contratos/*.yml` (`mvn compile` + validate)
    - _Depends: 2.1, 2.2_
  - _Requisitos: 8.1, 8.4, 8.5

- [x] 3. (P) Precio en el catálogo (módulo `pelicula`)
  - Añadir `precio` `BigDecimal` (`DECIMAL(10,2)`) a `Pelicula`; validaciones `@NotNull @Positive @Digits(integer=8, fraction=2)` en `PeliculaRequest`; `precio` en `PeliculaResponse` y en `PeliculaResumen` (raíz del módulo; null en películas preexistentes)
  - Pruebas unitarias del servicio e integración del controlador actualizadas
  - Observable: crear/editar sin precio o con precio ≤ 0 o con más de 2 decimales → 400; con precio válido se persiste y aparece en listado y detalle; `PeliculaResumen` expone `precio` para carrito/compra
  - _Boundary: pelicula_
  - _Requisitos: 1.1, 1.2, 1.3, 1.4, 1.5

- [ ] 4. Módulo `carrito`
  - [ ] 4.1 (P) Entidades `Carrito` (usuarioId único, `@Version`) y `LineaCarrito` (única por (carritoId, peliculaId)), `CarritoRepository` y doble de repositorio en memoria; añadir `carrito` y `compra` a `MODULOS` en `ArquitecturaTest`
    - Observable: ArchUnit pasa con los dos módulos declarados; restricciones únicas y `@Version` presentes en las entidades
    - _Boundary: carrito/domain_
  - [ ] 4.2 Oyente de `PeliculaEliminada` en carrito que borra las líneas de la película de todos los carritos + prueba del oyente
    - _Depends: 4.1_
    - Observable: al eliminar una película, sus líneas desaparecen de todos los carritos
  - [ ] 4.3 (P) `CarritoService` (ver: líneas con precio actual vía `PeliculaApi.resumenes` y total, vacío con total cero si no hay carrito, y omisión defensiva de líneas cuya película ya no exista; anadir: crea el carrito implícitamente con relectura ante carrera por UNIQUE, 404 película inexistente, 409 duplicada, 409 sin precio; quitar: 404 si no está; vaciar idempotente) + interfaz `CarritoApi` (`peliculasEnCarrito`, `vaciar`) + records de aplicación `LineaDeCarrito`/`CarritoVista` + pruebas unitarias con fakes propios (incluido el caso de línea huérfana omitida)
    - _Depends: 4.1, 3_
    - _Boundary: carrito/application_
    - Observable: las pruebas unitarias cubren todos los casos listados (incluida la omisión de líneas huérfanas) y pasan
  - [ ] 4.4 `CarritoController` implementando la interfaz generada `CarritoRestApi` (mapea DTOs generados ↔ records de aplicación) + pruebas de integración con la tabla completa de códigos (200/201/204/400/401/404/409) y cuerpo de error `ErrorRespuesta`
    - _Depends: 2.3, 4.3_
    - Observable: los cuatro endpoints responden según el catálogo de errores del diseño con token; 401 sin token
  - _Requisitos: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2, 4.3, 4.4

- [ ] 5. Módulo `compra`
  - [ ] 5.1 (P) Entidades `Compra` (usuarioId, fecha `LocalDateTime`, total, estado `COMPRADA`, índice usuario+fecha) y `LineaCompra` (compraId, peliculaId, titulo, precioUnitario snapshot), `CompraRepository` y doble
    - Observable: el historial conserva titulo y precio aunque la película se edite o elimine (verificado en pruebas unitarias)
    - _Boundary: compra/domain_
  - [ ] 5.2 `CompraService`: finalizar transaccional (lee carrito vía `CarritoApi`, valida comprables vía `PeliculaApi` con 409 si vacío/inexistente/sin precio, crea `Compra` con snapshot, vacía carrito; captura defensiva del bloqueo optimista solo si el flush ocurre dentro del método), listar propio por fecha descendente, detalle con 404 si ajena o inexistente + pruebas unitarias con fakes de `CarritoApi` y `PeliculaApi`
    - _Depends: 5.1, 4.3, 3_
    - _Boundary: compra/application_
    - Observable: las pruebas unitarias cubren el flujo feliz, los conflictos y, si el flush ocurre dentro del método, la conversión a `ConflictoException`
  - [ ] 5.3 `CompraController` implementando `ComprasRestApi` (convierte fecha a `date-time`) + pruebas de integración (201 con snapshot, 409 carrito vacío, 404 ajena)
    - _Depends: 2.3, 5.2_
    - Observable: los tres endpoints responden según el diseño con token
  - _Requisitos: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3

- [ ] 6. Concurrencia de checkout
  - [ ] 6.1 Mapeo de bloqueo optimista a 409 en `GlobalExceptionHandler` — mecanismo autoridad de 5.6, cubre el fallo en flush/commit aunque ocurra fuera del cuerpo de `finalizar` — + prueba del handler
    - _Boundary: compartido_
    - Observable: una excepción de bloqueo optimista produce 409 con el cuerpo estándar, venga de donde venga
  - [ ] 6.2 Prueba de integración del doble checkout simultáneo por la capa HTTP sobre el mismo carrito: exactamente una compra creada, la otra 409, carrito vacío al final (verifica el mecanismo real, no el doble de prueba)
    - _Depends: 5.3, 6.1_
  - _Requisitos: 5.6

- [ ] 7. Seguridad y cierre
  - [ ] 7.1 Extender `SeguridadIntegracionTest`: 401 sin token y con token caducado en las siete rutas nuevas (cuatro de carrito: `GET /api/carrito`, `POST /api/carrito/lineas`, `DELETE /api/carrito/lineas/{peliculaId}`, `DELETE /api/carrito`; tres de compras: `POST /api/compras`, `GET /api/compras`, `GET /api/compras/{compraId}`); verificar que una compra ajena da 404 y no 403; confirmar que ningún endpoint acepta usuario en cuerpo o ruta
    - Observable: la suite de seguridad cubre las 7 rutas y pasa
  - [ ] 7.2 Revisar la cobertura requisito↔prueba (mapeo en notas) y ejecutar `mvn test` completo incluida ArchUnit
  - _Requisitos: 7.1, 7.2, 7.3

## Implementation Notes
- 3: Feature Flag Protocol no aplicable (campo de esquema + Bean Validation no conmutables); RED = fallo de compilación + 12 tests rojos en `valoracion` (literales sin precio → 400), corregidos añadiendo `"precio":12.99` a los literales de `ValoracionControllerIntegracionTest`, `MarcaPeliculaControllerIntegracionTest` y `BorradoPeliculaIntegracionTest`. `PeliculaResumen` ahora (id, titulo, anio, genero, director, precio, valoracionMedia) — actualizar constructores posicionales en dobles. · 1.2, 1.5: la mitad "no comprable" (409) se verifica en 4.3/5.2.
- 1.1: el `spring` 7.26.0 ignora `useRecords` (solo lo implementa `aspnet-fastendpoints`); el soporte real se fusionó para 7.27.0 (PRs #25200..#25351, no liberado aún). Decisión revisada con el usuario (DTO generados como records): se usa `openapi-generator-maven-plugin 7.27.0-SNAPSHOT` con `pluginRepository` de snapshots de Central (https://central.sonatype.com/repository/maven-snapshots/). Al liberarse 7.27.0, fijar la versión y quitar el repositorio de snapshots. Jackson 3 (useJackson3) compila y serializa bien en Boot 4.1; no hizo falta el plan B. Warnings silenciados con `generateJsonIncludeAnnotations=false` y `generateJsonSetterNullsAnnotations=false`. Interfaz spike `SpikeMapeosGeneradosTest` (org.example.carrito) hay que eliminarla al llegar 4.4/5.3.
- 1.1: Maven no está en el PATH: `/opt/homebrew/Cellar/apache-maven-3.9.6/bin/mvn`.
- 1.2: si el alias sin versión del webjar no resuelve, usar la ruta con versión concreta en `swagger-ui.html`; `persistAuthorization` evita re-loguear en cada prueba manual.
- 2.x: el schema `ErrorRespuesta` se duplica en los dos YAML (solo se usa en anotaciones `@ApiResponse`, no en firmas); los errores reales los produce `GlobalExceptionHandler`.
- 4.1: añadir ambos módulos a `MODULOS` es seguro con `allowEmptyShould(true)` aunque `compra` aún no tenga clases.
- 4.3: crear fakes propios en el paquete de test de `carrito` (el `PeliculaApiFalso` de `valoracion` es de otro paquete); 3.6 (400 por cuerpo inválido) la cubre la validación del DTO generado + `GlobalExceptionHandler`, verificada en 4.4. No usar `@Validated` a nivel de clase (rompe el 400 de `HandlerMethodValidationException`).
- 4.4/5.3: los controllers implementan las interfaces generadas; el mapeo por `@RequestMapping` heredado de interfaz se verificó en 1.1.
- 5.2/5.3: la entidad guarda `LocalDateTime` y el DTO generado usa `date-time` (`OffsetDateTime`): conversión en la capa web.
- 6.x: con `@Version` en `Carrito`, el segundo checkout simultáneo falla en flush/commit; 6.1 garantiza el 409 sea cual sea el punto del fallo.
- General: BD de pruebas H2 compartida — los tests que creen películas/compras deben limpiar (ver `ValoracionControllerIntegracionTest`); `ddl-auto=update` crea tablas y columna `precio` sin migración manual; las películas preexistentes sin precio quedan no comprables (409) hasta que el admin las edite.
