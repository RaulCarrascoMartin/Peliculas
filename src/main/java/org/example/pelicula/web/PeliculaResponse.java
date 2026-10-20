package org.example.pelicula.web;

import org.example.pelicula.domain.Pelicula;

public record PeliculaResponse(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        Double valoracionMedia) {

    public static PeliculaResponse desde(Pelicula pelicula) {
        return new PeliculaResponse(pelicula.getId(), pelicula.getTitulo(), pelicula.getAnio(),
                pelicula.getGenero(), pelicula.getDirector(), pelicula.getValoracionMedia());
    }
}
