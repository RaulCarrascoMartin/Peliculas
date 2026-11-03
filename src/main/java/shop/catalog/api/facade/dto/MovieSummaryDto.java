package shop.catalog.api.facade.dto;

import java.math.BigDecimal;

/** Public data of a movie for other modules; avoids exposing the entity. */
public record MovieSummaryDto(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Double averageRating,
        int stock) {
}
