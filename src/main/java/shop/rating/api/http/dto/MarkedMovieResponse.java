package shop.rating.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import shop.rating.internal.application.dto.MarkedMovieView;

public record MarkedMovieResponse(
        Long id,
        @JsonProperty("titulo") String title,
        @JsonProperty("anio") Integer year,
        @JsonProperty("genero") String genre,
        String director,
        @JsonProperty("valoracionMedia") Double averageRating) {

    public static MarkedMovieResponse from(MarkedMovieView movie) {
        return new MarkedMovieResponse(movie.id(), movie.title(), movie.year(),
                movie.genre(), movie.director(), movie.averageRating());
    }
}
