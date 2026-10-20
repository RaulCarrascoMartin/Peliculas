package org.example.pelicula.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.pelicula.domain.Pelicula;

public record PeliculaRequest(
        @NotBlank String titulo,
        @NotNull @Min(1888) @Max(2100) Integer anio,
        @NotBlank String genero,
        @NotBlank String director) {

    public Pelicula aEntidad() {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(titulo);
        pelicula.setAnio(anio);
        pelicula.setGenero(genero);
        pelicula.setDirector(director);
        return pelicula;
    }
}
