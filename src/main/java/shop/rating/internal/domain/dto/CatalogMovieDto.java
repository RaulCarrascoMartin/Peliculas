package shop.rating.internal.domain.dto;

/** Catalog movie as the rating module sees it. */
public record CatalogMovieDto(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        Double averageRating) {
}
