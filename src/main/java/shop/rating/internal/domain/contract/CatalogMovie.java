package shop.rating.internal.domain.contract;

/** Catalog movie as the rating module sees it. */
public record CatalogMovie(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        Double averageRating) {
}
