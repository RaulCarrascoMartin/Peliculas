package org.example.pelicula.internal.web;

import org.example.pelicula.internal.domain.Pelicula;
import java.math.BigDecimal;

public record PeliculaResponse(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        BigDecimal precio,
        Double valoracionMedia) {

    public static PeliculaResponse desde(Pelicula pelicula) {
        return new PeliculaResponse(pelicula.getId(), pelicula.getTitulo(), pelicula.getAnio(),
                pelicula.getGenero(), pelicula.getDirector(), pelicula.getPrecio(),
                pelicula.getValoracionMedia());
    }
}
