package shop.rating.internal.domain.dto;

/** A user's rating of a movie as it travels between layers: controller, service and repository. */
public record RatingDto(
        Long id,
        Long userId,
        Long movieId,
        Integer score,
        String comment) {
}
