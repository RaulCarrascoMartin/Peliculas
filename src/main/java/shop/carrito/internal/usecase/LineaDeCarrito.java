package shop.carrito.internal.usecase;

import java.math.BigDecimal;

/** Línea del carrito tal y como la ve la web: con el título y el precio vivo de la película. */
public record LineaDeCarrito(
        Long peliculaId,
        String titulo,
        BigDecimal precio) {
}
