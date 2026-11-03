package shop.catalog.internal.application.dto;

import shop.catalog.internal.domain.model.MovieDetails;

import java.math.BigDecimal;

/** Editable data of a movie: input of the create and update use cases. A null stock is left untouched. */
public record MovieData(
        String title,
        Integer year,
        String genre,
        String director,
        BigDecimal price,
        Integer stock) {

    public MovieDetails toDetails() {
        return new MovieDetails(title, year, genre, director, price, stock);
    }
}
