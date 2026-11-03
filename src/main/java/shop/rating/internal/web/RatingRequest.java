package shop.rating.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull @Min(1) @Max(5) @JsonProperty("puntuacion") Integer score,
        @JsonProperty("comentario") String comment) {
}
