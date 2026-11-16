package shop.catalog.internal.domain.dto;

import java.math.BigDecimal;

/** Editable data of a movie. A {@code null} stock is left untouched on update and means 0 on create. */
public record MovieDetailsDto(
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Integer stock) {
}
