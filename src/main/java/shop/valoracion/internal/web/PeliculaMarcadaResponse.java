package shop.valoracion.internal.web;

import shop.pelicula.api.PeliculaResumen;

public record PeliculaMarcadaResponse(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        Double valoracionMedia) {

    public static PeliculaMarcadaResponse desde(PeliculaResumen pelicula) {
        return new PeliculaMarcadaResponse(pelicula.id(), pelicula.titulo(), pelicula.anio(),
                pelicula.genero(), pelicula.director(), pelicula.valoracionMedia());
    }
}
