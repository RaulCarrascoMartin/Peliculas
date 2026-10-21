package org.example.pelicula;

/** Datos públicos de una película para otros módulos; evita exponer la entidad. */
public record PeliculaResumen(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        Double valoracionMedia) {
}
