package shop.rating.internal.application.dto;

/** Movie in a user's list, as the list use case returns it. */
public record MarkedMovieView(
        Long id,
        String title,
        Integer year,
        String genre,
        String director,
        Double averageRating) {
}
