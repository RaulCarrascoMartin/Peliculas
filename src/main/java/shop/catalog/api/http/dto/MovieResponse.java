package shop.catalog.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
