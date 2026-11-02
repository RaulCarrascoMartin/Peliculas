package org.example.pelicula.internal.usecase;

import org.example.compartido.error.RecursoNoEncontradoException;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class EliminarPelicula {

    private final PeliculaRepository peliculas;

    public EliminarPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public void ejecutar(Long id) {
        if (!peliculas.existsById(id)) {
            throw new RecursoNoEncontradoException("Película " + id + " no encontrada");
        }
        peliculas.deleteById(id);
    }
}
