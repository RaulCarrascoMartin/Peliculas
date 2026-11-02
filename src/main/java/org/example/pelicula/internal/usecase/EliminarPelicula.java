package org.example.pelicula.internal.usecase;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.api.PeliculaEliminada;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarPelicula {

    private final PeliculaRepository peliculas;
    private final ApplicationEventPublisher eventos;

    public EliminarPelicula(PeliculaRepository peliculas, ApplicationEventPublisher eventos) {
        this.peliculas = peliculas;
        this.eventos = eventos;
    }

    /** Borra la película y avisa con {@link PeliculaEliminada}; los oyentes actúan en la misma transacción. */
    @Transactional
    public void ejecutar(Long id) {
        if (!peliculas.existsById(id)) {
            throw new RecursoNoEncontradoException("Película " + id + " no encontrada");
        }
        peliculas.deleteById(id);
        eventos.publishEvent(new PeliculaEliminada(id));
    }
}
