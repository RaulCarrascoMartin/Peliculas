package shop.rating.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RatingResponse(
        Long id,
        @JsonProperty("peliculaId") Long movieId,
        @JsonProperty("puntuacion") Integer score,
        @JsonProperty("comentario") String comment) {
}
