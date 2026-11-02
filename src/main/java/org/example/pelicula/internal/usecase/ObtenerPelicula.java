package org.example.pelicula.internal.usecase;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class ObtenerPelicula {

    private final PeliculaRepository peliculas;

    public ObtenerPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public Pelicula ejecutar(Long id) {
        return peliculas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película " + id + " no encontrada"));
    }
}
