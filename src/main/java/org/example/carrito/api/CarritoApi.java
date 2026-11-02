package org.example.carrito.api;

import java.util.List;

/** API pública del módulo {@code carrito}: lo único que otros módulos (compra) pueden usar. */
public interface CarritoApi {

    /** Ids de las películas que el usuario tiene en el carrito, en orden de inserción. */
    List<Long> peliculasEnCarrito(Long usuarioId);

    /** Deja el carrito del usuario sin líneas; idempotente. */
    void vaciar(Long usuarioId);
}
