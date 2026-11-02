package org.example.compra.internal.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una compra con sus líneas congeladas, tal como la devuelven los casos de uso de compra. */
public record CompraVista(
        Long id,
        Long usuarioId,
        LocalDateTime fecha,
        BigDecimal total,
        String estado,
        List<LineaCompraVista> lineas) {
}
