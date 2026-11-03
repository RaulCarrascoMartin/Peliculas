package shop.catalog.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import java.math.BigDecimal;

public record MovieResponse(
        Long id,
        @JsonProperty("titulo") String title,
        @JsonProperty("anio") Integer year,
        @JsonProperty("genero") String genre,
        String director,
        @JsonProperty("precio") BigDecimal price,
        @JsonProperty("valoracionMedia") Double averageRating,
        int stock) {

    public static MovieResponse from(MovieSummaryDto summary) {
        return new MovieResponse(summary.id(), summary.title(), summary.year(), summary.genre(), summary.director(),
                summary.price(), summary.averageRating(), summary.stock());
    }
}
