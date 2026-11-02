package org.example.compra.internal.usecase;

import java.math.BigDecimal;

/** Línea congelada de una compra: título y precio tal y como eran al comprar. */
public record LineaCompraVista(
        Long peliculaId,
        String titulo,
        BigDecimal precioUnitario) {
}
