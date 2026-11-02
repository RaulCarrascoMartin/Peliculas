package org.example.pelicula.internal.usecase;

import org.example.compartido.error.RecursoNoEncontradoException;
import org.example.pelicula.api.PeliculaResumen;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class ObtenerPelicula {

    private final PeliculaRepository peliculas;

    public ObtenerPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public PeliculaResumen ejecutar(Long id) {
        return peliculas.findById(id)
                .map(ResumenDePelicula::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película " + id + " no encontrada"));
    }
}
