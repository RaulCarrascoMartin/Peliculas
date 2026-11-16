package shop.catalog.internal.domain.dto;

import java.math.BigDecimal;

/** Reserved movie with the title and unit price at that moment. */
public record ReservationDto(
        Long movieId,
        String title,
        BigDecimal unitPrice,
        int quantity) {
}
