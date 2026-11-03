package shop.catalog.internal.usecase;

import java.math.BigDecimal;

/** Editable data of a movie: input of the create and update use cases. A null stock is left untouched. */
public record MovieData(
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Integer stock) {
}
