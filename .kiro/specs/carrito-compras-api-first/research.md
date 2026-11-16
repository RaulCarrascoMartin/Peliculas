# Research & Design Decisions — carrito-compras-api-first

## Summary
- **Feature**: `carrito-compras-api-first`
- **Discovery Scope**: Extension (codebase brownfield: módulos `pelicula`, `valoracion`, `autenticacion` + `compartido` existentes)
- **Key Findings**:
  - El generador `spring` de openapi-generator soporta Spring Boot 4 de forma nativa mediante la opción `useSpringBoot4` (estable desde ~7.23–7.26); la última versión estable del plugin es **7.26.0** (6 oct 2026).
  - Spring Boot 4 usa Jackson 3 por defecto; el generador ofrece `useJackson3=true` (solo permitido con `useSpringBoot4`), además de `useRecords=true` encajando con el estilo record de los DTO del proyecto.
  - El resto de la integración (JWT con `JwtFilter`/`UsuarioActual`, `GlobalExceptionHandler`, patrón `XApi`, evento `PeliculaEliminada`, repositorios falsos de prueba) ya existe y se reutiliza sin cambios estructurales.

## Research Log

### Compatibilidad del generador de código con Spring Boot 4.1 / Framework 7
- **Context**: Único riesgo técnico real identificado en la conversación de descubrimiento: ¿genera openapi-generator código compilable contra Boot 4.1 (jakarta, Jackson 3, Spring 7)?
- **Sources Consulted**: Releases oficiales de `OpenAPITools/openapi-generator` (GitHub) y documentación del generador `spring` (`docs/generators/spring.md`).
- **Findings**:
  - Última estable: v7.26.0. El generador `spring` es STABLE, tipo SERVER.
  - Opción `useSpringBoot4` (por defecto `false`): "Generate code and provide dependencies for use with Spring Boot 4.x. (Use jakarta instead of javax)". Activa `useJakartaEe`.
  - Opción `useJackson3`: dependencias Jackson 3, "only allowed when useSpringBoot4 is set". Historial de releases con fixes para Boot 4: #23649 (JSpecify nullable con Boot 4), #24709 (Jackson3 + SpringBoot 4), #24326 (spring-cloud Boot 4).
  - Opción `useRecords`: modelos como records Java (requiere Boot 3/4); encaja con la convención DTO-record del proyecto.
  - Opciones clave para modo interfaz: `interfaceOnly`, `useTags`, `requestMappingMode=api_interface`, `skipDefaultInterface`, `useResponseEntity=false` (`@ResponseStatus` en vez de `ResponseEntity`), `useBeanValidation=true` (por defecto), `documentationProvider=none` y `annotationLibrary=none` (sin dependencias springdoc/swagger-annotations), `useSwaggerUI=false` (la UI la servimos nosotros), `openApiNullable=false` (evita `jackson-databind-nullable`).
  - Mojo `validate` añadido en 7.24 para validar el contrato en el build; hash estable del inputSpec en 7.25 (regeneración incremental fiable).
- **Implications**: El spike de compatibilidad queda como primera tarea pero el riesgo baja: la ruta soportada oficialmente es `useSpringBoot4=true` + `useJackson3=true`. Se verifica en el spike que compila con Java 26 y que el `@RequestMapping` heredado de la interfaz funciona en Spring MVC 7.

### Versiones de swagger-ui y servicio estático
- **Context**: Req 8 exige página interactiva que muestre el contrato y permita probar con token.
- **Sources Consulted**: Convenciones conocidas de `org.webjars:swagger-ui` y de Spring Boot static resources; decisión de servir el YAML de contrato desde `src/main/resources/static/contratos/`.
- **Findings**:
  - Webjar `org.webjars:swagger-ui` (rama 5.x, última estable); `SwaggerUIBundle` soporta `urls: [{url, name}]` para varias specs con desplegable, y `persistAuthorization: true` para conservar el token entre recargas.
  - Spring Boot sirve `/webjars/**` desde `META-INF/resources/webjars`; el alias sin versión de los recursos del webjar requiere `webjars-locator(-lite)`: si no estuviera disponible, se usa la ruta con versión concreta.
  - El YAML no debe colgar de `/api/**` porque `anyRequest().authenticated()` lo exigiría con token.
- **Implications**: Dos ficheros de contrato servidos en `/contratos/*.yml`, una sola página estática `swagger-ui.html`, y `permitAll` en `SecurityConfig` para `/swagger-ui.html`, `/webjars/**` y `/contratos/**`.

### Análisis de la integración con el codebase existente
- **Context**: Extension — los puntos de extensión ya se exploraron en la conversación (lectura de `SecurityConfig`, `JwtFilter`, `UsuarioActual`, `GlobalExceptionHandler`, `PeliculaApi/PeliculaResumen`, `LimpiezaPorPeliculaEliminada`, `ArquitecturaTest`, `RepositorioEnMemoria`).
- **Sources Consulted**: Código fuente del proyecto + `.kiro/steering/structure.md` + spec `peliculas` (requirements/design).
- **Findings**:
  - `SecurityConfig` ya cubre `/api/carrito/**` y `/api/compras/**` con `anyRequest().authenticated()`; identidad vía `UsuarioActual.id()`.
  - `GlobalExceptionHandler` ya mapea `ValidacionException`→400, `RecursoNoEncontradoException`→404, `ConflictoException`→409, `AuthenticationException`→401; solo falta el caso de bloqueo optimista (→409).
  - El patrón de módulos (raíz con `XApi` + eventos, capas web/application/domain, entidades anémicas, DTOs record en `web`) es replicable; `RepositorioEnMemoria` y los fakes existentes sirven para las pruebas unitarias.
  - Las relaciones entre módulos se guardan como ids (sin `@ManyToOne` cruzado) según `structure.md`.
- **Implications**: Los módulos `carrito` y `compra` siguen el patrón al pie de la letra; los cambios en módulos existentes se reducen a: precio en `pelicula`, `permitAll` en `SecurityConfig`, handler de bloqueo optimista, plugin y webjar en `pom.xml`, y `MODULOS` en `ArquitecturaTest`.

## Architecture Pattern Evaluation

| Option | Description | Strengths | Risks / Limitations | Notes |
|--------|-------------|-----------|---------------------|-------|
| Monolito modular (actual) + 2 módulos nuevos | `carrito` y `compra` con capas web/application/domain y APIs raíz | Coherente con steering y ArchUnit; transacción local para checkout | Ninguno relevante | Seleccionado |
| springdoc (spec runtime) | Generar la OpenAPI desde el código y servirla | Cero codegen | Deriva a code-first; segunda fuente de verdad; Req 8 exige contrato como fuente | Rechazado |
| Contrato único con un módulo único de tienda | Un módulo `tienda` con carrito+compra y un solo YAML | Menos piezas | Rompe la separación de agregados y la granularidad de módulos del proyecto | Rechazado (el gate de ArchUnit por paquetes obliga a dos ejecuciones de generación de todos modos) |

## Design Decisions

### Decision: openapi-generator en modo interfaz con dos ejecuciones (una por módulo)
- **Context**: El contrato debe ser la fuente de verdad (Req 8) y las reglas ArchUnit prohíben que `compra` dependa del `web` de `carrito`.
- **Alternatives Considered**:
  1. Un YAML + una ejecución con tags — todos los interfaces y DTO generados caerían en el mismo paquete, mezclando módulos o violando ArchUnit.
  2. Un YAML con tags `Carrito Web`/`Compras Web` y un paquete por tag — no soportado por el generador (paquete único por ejecución).
  3. Dos YAML + dos ejecuciones del plugin, una por módulo — cada ejecución genera en `org.example.{modulo}.web.api`/`.dto`.
- **Selected Approach**: Opción 3. `carrito-api.yml` (paths `/api/carrito/**`, tag `CarritoRest`) y `compras-api.yml` (paths `/api/compras/**`, tag `ComprasRest`), ambos bajo `src/main/resources/static/contratos/` (servidos y usados como `inputSpec`, sin copias). Swagger UI los lista con `urls`.
- **Rationale**: Respeta ArchUnit (cada módulo implementa su interfaz generada), mantiene un único origen por contrato y da nombres sin colisión: la API de módulo se llama `CarritoApi` (raíz, patrón `XApi`) y el interfaz HTTP generado se llama `CarritoRestApi` (tag `CarritoRest`).
- **Trade-offs**: El schema `ErrorRespuesta` se duplica en los dos YAML (6 líneas); aceptable frente a la complejidad de $ref multi-fichero.
- **Follow-up**: Verificar en el spike que `requestMappingMode=api_interface` + controller que implementa la interfaz mapea correctamente en Spring MVC 7.

### Decision: precio vivo en carrito y snapshot en compra
- **Context**: Req 2.3 (precio del carrito se actualiza) y Req 5.3 (la compra congela el precio).
- **Alternatives Considered**:
  1. `LineaCarrito` guarda precio y se sincroniza con cambios del catálogo.
  2. `LineaCarrito` no guarda precio; se calcula al leer vía `PeliculaApi`.
- **Selected Approach**: Opción 2. `LineaCarrito` solo referencia `peliculaId`; `Compra`/`LineaCompra` snapshot `titulo` + `precioUnitario` + `total` en el checkout.
- **Rationale**: El comportamiento requerido se obtiene por construcción (imposible que el carrito quede desfasado) y el historial de compras es inmutable aunque la película se edite o elimine.
- **Trade-offs**: Cada lectura de carrito hace una llamada a `PeliculaApi.resumenes(ids)`; volumen de un carrito personal hace irrelevante el coste.

### Decision: concurrencia con bloqueo optimista en `Carrito`
- **Context**: Req 5.6 (doble checkout → una sola compra) y carrera de creación del carrito al añadir la primera línea.
- **Alternatives Considered**:
  1. Bloqueos pesimistas / serialización por usuario.
  2. `@Version` en `Carrito` + restricción UNIQUE(`usuario_id`).
- **Selected Approach**: Opción 2. El checkout falla con `ObjectOptimisticLockingFailureException` si otro checkout vació el carrito; se mapea a 409 en `GlobalExceptionHandler`. La creación concurrente del carrito se resuelve con UNIQUE + reintento de búsqueda.
- **Rationale**: Sin estado distribuido, misma BD; mecanismo JPA estándar y observable como 409.
- **Trade-offs**: Requiere añadir un handler a `GlobalExceptionHandler` (compartido) — afecta globalmente pero solo aplica a entidades con `@Version`.

### Decision: sin eventos ni abstracciones "por si acaso"
- **Context**: Simplificación (lente 3 de síntesis).
- **Alternatives Considered**: Publicar `CompraRealizada`; interfaz `CompraApi`; idempotency keys.
- **Selected Approach**: Ninguno. `compra` no expone `Api` (nadie la consume), no se publica evento (no hay consumidor), sin cabeceras de idempotencia (el bloqueo optimista cubre el doble envío observable).
- **Rationale**: La síntesis exige el diseño más pequeño que cumpla los requisitos; las interfaces pueden añadirse cuando exista consumidor.
- **Trade-offs**: Una futura estadística de ventas exigirá añadir `CompraApi` o el evento entonces.

## Risks & Mitigations
- Riesgo: el spike de generación con Boot 4.1/Java 26 revela incompatibilidad de plantillas (Jackson 3 o anotaciones). — Mitigación: spike como tarea 1; plan B documentado: `useSpringBoot4=true` + Jackson 2 (Boot 4 lo soporta), o `useSpringBoot3` no aplica (proyecto en 4.1).
- Riesgo: alias sin versión del webjar swagger-ui no resuelto. — Mitigación: fijar la ruta con versión concreta del webjar en `swagger-ui.html`.
- Riesgo: películas preexistentes sin precio tras `ddl-auto=update`. — Mitigación: explícito en requisitos (no comprables, 409) y el admin las rellena al editarlas (el precio pasa a ser obligatorio en la API).
- Riesgo: anotaciones de mapeo heredadas de interfaz generada no registradas por Spring. — Mitigación: verificado en el spike; `requestMappingMode=api_interface` es el modo documentado para `interfaceOnly`.

## References
- [Releases de openapi-generator](https://github.com/OpenAPITools/openapi-generator/releases) — versiones y soporte Boot 4/Jackson 3 (PRs #23649, #24709).
- [Documentación del generador spring](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/spring.md) — opciones `useSpringBoot4`, `useJackson3`, `useRecords`, `interfaceOnly`, `requestMappingMode`, `documentationProvider`.
- `.kiro/steering/structure.md`, `.kiro/steering/tech.md`, `.kiro/steering/security.md` — reglas de módulos, stack y seguridad del proyecto.
- `.kiro/specs/peliculas/design.md` — estilo y convenciones de diseño del proyecto.
