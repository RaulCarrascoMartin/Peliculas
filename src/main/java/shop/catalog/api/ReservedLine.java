package shop.catalog.api;

import java.math.BigDecimal;

/** Línea reservada con el título y el precio unitario capturados en el momento de la reserva. */
public record ReservedLine(
        Long productId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}
