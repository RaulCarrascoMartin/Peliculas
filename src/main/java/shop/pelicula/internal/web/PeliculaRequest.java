package shop.pelicula.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import shop.pelicula.internal.usecase.DatosPelicula;

import java.math.BigDecimal;

public record PeliculaRequest(
        @NotBlank String titulo,
        @NotNull @Min(1888) @Max(2100) Integer anio,
        @NotBlank String genero,
        @NotBlank String director,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal precio) {

    public DatosPelicula aDatos() {
        return new DatosPelicula(titulo, anio, genero, director, precio);
    }
}
