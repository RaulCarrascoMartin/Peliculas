package shop.rating.internal.application.dto;

/** A user's rating of a movie, as returned by the rate use case. */
public record RatingView(
        Long id,
        Long movieId,
        Integer score,
        String comment) {
}
