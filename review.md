# Revisión: refactor/reglas-vault-backend (monolito modular)

## Bloqueantes
Sin hallazgos

## Importantes
- [x] src/main/java/shop/catalog/internal/domain/service/MovieServiceImpl.java:92 (`updateAverageRating`) y src/main/java/shop/catalog/internal/persistence/repository/MovieRepositoryImpl.java:62 (`save`, UPDATE) — escritura condicional / lost update: el servicio lee la película completa, cambia solo `averageRating` y `save` reescribe todas las columnas, incluido `stock`. Si un checkout descuenta stock con `decrementStock` entre la lectura y el UPDATE (o un administrador edita el stock), la valoración pisa el stock con un valor viejo y se pierde la reserva. Lo mismo ocurre en `update()` (línea 44) con `valoracion_media`, que el contrato dice que no se edita. — Añadir a `MovieRepository` un `updateAverageRating(id, average)` con `update peliculas set valoracion_media = :avg where id = :id` y comprobar filas afectadas (0 lanza `MovieNotFoundException`), y que el UPDATE de `save` no toque `stock` ni `valoracion_media` salvo que el cambio de stock sea explícito (p. ej. `setStock` propio).

## Menores
- [x] src/main/java/shop/rating/internal/persistence/repository/RatingRepositoryImpl.java:55 y src/main/java/shop/rating/internal/persistence/repository/MovieMarkRepositoryImpl.java:52 — escrituras condicionales: el `update` ignora las filas afectadas; si la fila desapareció (por ejemplo, borrado de la película en paralelo), `save` devuelve el DTO como si se hubiera guardado — Comprobar `update() == 0` y lanzar una excepción de dominio (not found / conflicto).
- [x] src/main/java/shop/shared/security/JwtFilter.java:1 — toda clase con su prueba: no existe `JwtFilterTest` directo (solo se ejercita indirectamente en `SecurityIntegrationTest`) — Añadir una prueba unitaria: token válido establece el contexto, ausente o inválido lo deja vacío.
- [ ] src/main/java/shop/catalog/internal/persistence/repository/MovieRepositoryImpl.java:88 — escrituras condicionales: `deleteById` ignora las filas afectadas (el servicio ya comprueba `existsById`, pero hay una ventana de carrera) — Comprobar el resultado o devolver `boolean` y que el servicio lance `MovieNotFoundException`. — DESCARTADA: borrar es idempotente; si la fila ya no existe el resultado final es el mismo y el servicio ya comprueba `existsById`.

## Comprobado
- Dependencias entre capas: sin imports de `domain` hacia `api.http` ni `persistence`, ni de controller hacia persistence; inyección por constructor con campos `final` en servicios, controllers y repositorios.
- Usuario siempre desde `CurrentUser` en los controllers de cart, purchase, orders, rating y marks; órdenes y compras filtran por propietario (`OrderServiceImpl.load` oculta recursos ajenos, `findByIdAndUserId` en purchase); `MovieController` con `@PreAuthorize` de ADMIN en las escrituras.
- Transacciones presentes en todos los `*ServiceImpl` que escriben; escrituras condicionales correctas en stock (`stock >= :q`), cancelación de pedido (`status = CREATED`), bloqueo optimista del carrito y borrado de línea.
- Modelos anémicos con reglas en el servicio, MapStruct en mappers y SQL parametrizado (el `orderBy` usa lista cerrada). Todas las clases modificadas tienen prueba salvo lo anotado arriba.
