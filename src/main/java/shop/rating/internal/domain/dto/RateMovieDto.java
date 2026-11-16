package shop.rating.internal.domain.dto;

/** Input of rating a movie: who rates, which movie, the score and an optional comment. */
public record RateMovieDto(
        Long userId,
        Long movieId,
        int score,
        String comment) {
}
