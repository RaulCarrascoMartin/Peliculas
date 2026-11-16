package shop.rating.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarkedMovieResponse(
        Long id,
        @JsonProperty("titulo") String title,
        @JsonProperty("anio") Integer year,
        @JsonProperty("genero") String genre,
        String director,
        @JsonProperty("valoracionMedia") Double averageRating) {
}
