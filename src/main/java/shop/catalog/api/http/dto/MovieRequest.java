package shop.catalog.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body of the creation and of the update of a movie; the stock is optional (see the service for what it means). */
public record MovieRequest(
        @JsonProperty("titulo") @NotBlank String title,
        @NotNull @Min(1888) @Max(2100) @JsonProperty("anio") Integer year,
        @JsonProperty("genero") @NotBlank String genre,
        @NotBlank String director,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) @JsonProperty("precio") BigDecimal price,
        @Min(0) Integer stock) {
}
