package org.example.pelicula.internal.web;

import org.example.pelicula.api.PeliculaResumen;
import java.math.BigDecimal;

public record PeliculaResponse(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        BigDecimal precio,
        Double valoracionMedia) {

    public static PeliculaResponse desde(PeliculaResumen p) {
        return new PeliculaResponse(p.id(), p.titulo(), p.anio(), p.genero(), p.director(), p.precio(),
                p.valoracionMedia());
    }
}
