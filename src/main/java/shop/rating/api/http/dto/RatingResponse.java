package shop.rating.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import shop.rating.internal.application.dto.RatingView;

public record RatingResponse(
        Long id,
        @JsonProperty("peliculaId") Long movieId,
        @JsonProperty("puntuacion") Integer score,
        @JsonProperty("comentario") String comment) {

    public static RatingResponse from(RatingView view) {
        return new RatingResponse(view.id(), view.movieId(), view.score(), view.comment());
    }
}
