# Requisitos

## Introducción
Carrito de compra y proceso de compra de películas para la API existente. Un usuario autenticado añade películas con precio a su carrito, las quita, vacía el carrito y finaliza la compra con pago simulado; consulta después el historial de sus compras. El administrador gestiona el precio de las películas. Un carrito solo indica qué películas quiere comprar el usuario (una copia por película, sin cantidades). Los endpoints se definen primero en un contrato de API que sirve de fuente de verdad y se puede explorar y probar de forma interactiva.

## Contexto de frontera
- **In scope**: consultar, añadir, quitar y vaciar el carrito propio; finalizar la compra (checkout) con pago simulado y precio congelado en la compra; historial y detalle de las compras propias; gestión del precio de las películas por el administrador; página interactiva para ver y probar el contrato de la API.
- **Out of scope**: pasarela de pago real o estados de pago pendientes; cantidades mayores de uno; descuentos, impuestos y divisas; carritos de usuarios anónimos; reembolsos, devoluciones o anulación de compras; notificaciones o envíos.
- **Expectativas adyacentes**: el catálogo (spec `peliculas`) gana el campo precio en creación, edición y detalle; la autenticación existente (registro, login y token) provee la identidad del usuario; el formato de error uniforme ya existente se reutiliza para los nuevos endpoints.

## Requisito 1: Precio en el catálogo

**Historia de usuario:** Como administrador, quiero fijar el precio de las películas, para poder venderlas.

### Criterios de aceptación
1. WHEN el administrador crea una película con un precio mayor que cero y con dos decimales THE SYSTEM SHALL guardarla con ese precio y devolverla.
2. IF el administrador crea o edita una película sin precio, con un precio menor o igual que cero, o con más de dos decimales THEN THE SYSTEM SHALL rechazar la petición con un error de validación.
3. WHEN el administrador edita el precio de una película existente THE SYSTEM SHALL actualizarlo y devolver la película modificada.
4. WHEN cualquier usuario consulta el catálogo o el detalle de una película THE SYSTEM SHALL incluir su precio.
5. WHEN existe una película sin precio (creada antes de este cambio) THE SYSTEM SHALL considerarla no comprable.

## Requisito 2: Consultar el carrito

**Historia de usuario:** Como usuario, quiero ver mi carrito con su total, para saber qué voy a comprar y cuánto cuesta.

### Criterios de aceptación
1. WHEN un usuario consulta su carrito y todavía no lo ha creado THE SYSTEM SHALL devolver un carrito vacío con total cero.
2. WHEN un usuario consulta su carrito con películas THE SYSTEM SHALL devolver cada película con su precio actual y el total del carrito.
3. WHEN el precio de una película cambia y el usuario consulta su carrito THE SYSTEM SHALL mostrar el precio nuevo en la línea y en el total.
4. WHEN un usuario consulta su carrito THE SYSTEM SHALL devolver solo las películas que él añadió.

## Requisito 3: Añadir películas al carrito

**Historia de usuario:** Como usuario, quiero añadir películas a mi carrito, para preparar mi compra.

### Criterios de aceptación
1. WHEN un usuario añade a su carrito una película existente y comprable THE SYSTEM SHALL añadirla y devolver la línea con su precio actual.
2. WHEN un usuario sin carrito añade su primera película THE SYSTEM SHALL crear el carrito con esa línea.
3. IF la película ya está en su carrito THEN THE SYSTEM SHALL rechazar la petición con un error de conflicto.
4. IF la película no existe en el catálogo THEN THE SYSTEM SHALL responder con un error de recurso no encontrado.
5. IF la película no tiene precio THEN THE SYSTEM SHALL rechazar la petición con un error de conflicto.
6. IF el cuerpo de la petición no incluye un identificador de película válido THEN THE SYSTEM SHALL rechazar la petición con un error de validación.

## Requisito 4: Quitar películas y vaciar el carrito

**Historia de usuario:** Como usuario, quiero quitar películas de mi carrito o vaciarlo entero, para rectificar antes de comprar.

### Criterios de aceptación
1. WHEN un usuario quita una película de su carrito THE SYSTEM SHALL eliminar la línea y responder sin contenido.
2. IF la película indicada no está en su carrito THEN THE SYSTEM SHALL responder con un error de recurso no encontrado.
3. WHEN un usuario vacía su carrito, tenga líneas o no THE SYSTEM SHALL dejarlo sin líneas y responder sin contenido.
4. WHEN se elimina del catálogo una película que está en algún carrito THE SYSTEM SHALL quitarla de todos los carritos.

## Requisito 5: Finalizar la compra

**Historia de usuario:** Como usuario, quiero finalizar la compra de mi carrito, para quedarme con las películas al precio de ese momento.

### Criterios de aceptación
1. WHEN un usuario finaliza la compra de un carrito con películas comprables THE SYSTEM SHALL crear una compra con estado comprada, guardar en cada línea el título, el precio unitario vigente en ese momento y el total, vaciar el carrito y devolver la compra creada.
2. WHEN el precio de una película cambió desde que se añadió al carrito THE SYSTEM SHALL usar en la compra el precio vigente al finalizarla.
3. WHEN el precio de una película comprada sube después de la compra THE SYSTEM SHALL mantener en la compra el precio con el que se compró.
4. IF el carrito del usuario está vacío o no existe THEN THE SYSTEM SHALL rechazar la compra con un error de conflicto.
5. IF una película del carrito desaparece del catálogo o deja de ser comprable en el momento de finalizar THEN THE SYSTEM SHALL rechazar la compra con un error de conflicto y dejar el carrito sin cambios.
6. WHEN dos peticiones de finalización del mismo carrito se procesan a la vez THE SYSTEM SHALL crear una sola compra y rechazar la otra con un error de conflicto.

## Requisito 6: Historial de compras

**Historia de usuario:** Como usuario, quiero consultar mis compras, para ver qué he comprado y a qué precio.

### Criterios de aceptación
1. WHEN un usuario consulta sus compras THE SYSTEM SHALL devolver solo las suyas, de la más reciente a la más antigua.
2. WHEN un usuario consulta una compra suya por su identificador THE SYSTEM SHALL devolverla con su estado, fecha, líneas (título, precio unitario) y total.
3. IF la compra solicitada no existe o pertenece a otro usuario THEN THE SYSTEM SHALL responder con un error de recurso no encontrado.

## Requisito 7: Acceso y propiedad

**Historia de usuario:** Como usuario, quiero que mi carrito y mis compras sean privados, para que nadie más pueda verlos ni modificarlos.

### Criterios de aceptación
1. IF una petición al carrito o a las compras llega sin token, con un token inválido o caducado THEN THE SYSTEM SHALL responder con un error de no autenticado.
2. WHEN un usuario actúa sobre su carrito o sus compras THE SYSTEM SHALL identificarle a partir de su token y no aceptar ningún identificador de usuario en la petición.
3. THE SYSTEM SHALL no permitir operar sobre el carrito o las compras de otro usuario, ni siquiera al administrador.

## Requisito 8: Contrato y documentación de la API

**Historia de usuario:** Como desarrollador que consume la API, quiero un contrato publicado y una página para probar los endpoints, para integrarme sin leer el código.

### Criterios de aceptación
1. THE SYSTEM SHALL publicar el contrato de los endpoints de carrito y compras (rutas, cuerpos, códigos de respuesta y errores) como documento accesible sin autenticación.
2. THE SYSTEM SHALL ofrecer una página interactiva que muestre esos endpoints y permita ejecutar peticiones de prueba contra la API.
3. WHEN el usuario se autentica en esa página con su token THE SYSTEM SHALL usar ese token en las peticiones de prueba que lance desde ella.
4. WHEN cualquier endpoint de carrito o compras responde con error THE SYSTEM SHALL usar el cuerpo de error uniforme (estado, mensaje y errores) que el contrato documenta.
5. THE SYSTEM SHALL comportarse exactamente como el contrato documenta; si ambos discrepan, el contrato manda.
