# Requisitos

## Introducción
API REST para gestionar un catálogo de películas. Un administrador mantiene el catálogo; el público lo consulta, busca y valora películas, y mantiene listas de favoritas y vistas.

## Requisito 0: Autenticación

**Historia de usuario:** Como usuario, quiero registrarme e iniciar sesión, para valorar películas y gestionar mis listas.

### Criterios de aceptación
1. WHEN un visitante se registra con un email no usado y una contraseña válida THE SYSTEM SHALL crear su cuenta con rol de usuario y guardar la contraseña cifrada.
2. IF el email ya está registrado THEN THE SYSTEM SHALL rechazar el registro con un error de conflicto.
3. WHEN un usuario envía credenciales correctas THE SYSTEM SHALL devolver un token JWT con su identificador y rol.
4. IF las credenciales son incorrectas THEN THE SYSTEM SHALL responder con un error de no autenticado, sin indicar si falló el email o la contraseña.
5. IF una petición protegida llega sin token, con un token inválido o caducado THEN THE SYSTEM SHALL responder con un error de no autenticado.

## Requisito 1: Gestionar el catálogo

**Historia de usuario:** Como administrador, quiero crear, editar y eliminar películas, para mantener el catálogo actualizado.

### Criterios de aceptación
1. WHEN el administrador envía una película con título, año, género y director válidos THE SYSTEM SHALL guardarla y devolver la película creada con su identificador.
2. WHEN el administrador envía una película sin título o con un año inválido THE SYSTEM SHALL rechazar la petición con un error de validación.
3. WHEN el administrador edita una película existente THE SYSTEM SHALL actualizar sus datos y devolver la película modificada.
4. WHEN el administrador elimina una película existente THE SYSTEM SHALL eliminarla del catálogo junto con sus valoraciones.
5. IF la película solicitada no existe THEN THE SYSTEM SHALL responder con un error de recurso no encontrado.
6. IF un usuario que no es administrador intenta crear, editar o eliminar una película THEN THE SYSTEM SHALL denegar la operación.

## Requisito 2: Consultar el catálogo

**Historia de usuario:** Como usuario del público, quiero consultar las películas, para conocer qué hay disponible.

### Criterios de aceptación
1. WHEN el usuario solicita el listado THE SYSTEM SHALL devolver las películas paginadas.
2. WHEN el usuario solicita una película por su identificador THE SYSTEM SHALL devolver su detalle, incluida su valoración media.

## Requisito 3: Buscar y filtrar

**Historia de usuario:** Como usuario del público, quiero buscar y filtrar películas, para encontrar rápido la que me interesa.

### Criterios de aceptación
1. WHEN el usuario busca por texto THE SYSTEM SHALL devolver las películas cuyo título contenga el texto, sin distinguir mayúsculas.
2. WHEN el usuario filtra por género, año o director THE SYSTEM SHALL devolver solo las películas que cumplan todos los filtros indicados.
3. IF ninguna película cumple la búsqueda THEN THE SYSTEM SHALL devolver una lista vacía.

## Requisito 4: Valorar películas

**Historia de usuario:** Como usuario del público, quiero puntuar y comentar películas, para compartir mi opinión.

### Criterios de aceptación
1. WHEN un usuario envía una puntuación de 1 a 5 para una película existente THE SYSTEM SHALL guardar su valoración.
2. IF la puntuación está fuera del rango de 1 a 5 THEN THE SYSTEM SHALL rechazar la petición con un error de validación.
3. WHEN un usuario valora de nuevo una película que ya había valorado THE SYSTEM SHALL actualizar su valoración anterior, sin crear otra.
4. WHEN se guarda o cambia una valoración THE SYSTEM SHALL recalcular la valoración media de la película.

## Requisito 5: Listas de favoritas y vistas

**Historia de usuario:** Como usuario del público, quiero marcar películas como favoritas o vistas, para llevar el control de mi colección.

### Criterios de aceptación
1. WHEN un usuario marca una película como favorita o vista THE SYSTEM SHALL añadirla a la lista correspondiente.
2. WHEN un usuario la desmarca THE SYSTEM SHALL quitarla de esa lista.
3. WHEN un usuario solicita su lista de favoritas o vistas THE SYSTEM SHALL devolver solo sus propias películas.
