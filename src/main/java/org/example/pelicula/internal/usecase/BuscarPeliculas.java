package org.example.pelicula.internal.usecase;

import org.example.pelicula.api.PeliculaResumen;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BuscarPeliculas {

    private final PeliculaRepository peliculas;

    public BuscarPeliculas(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    /** Lista paginada; los filtros nulos o en blanco se ignoran y los indicados se combinan con "y". */
    public Page<PeliculaResumen> ejecutar(String titulo, String genero, Integer anio, String director,
                                          Pageable paginacion) {
        return peliculas.buscar(texto(titulo), texto(genero), anio, texto(director), paginacion)
                .map(ResumenDePelicula::de);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
