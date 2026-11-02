package org.example.valoracion.internal.usecase;

import org.example.pelicula.api.PeliculaEliminada;
import org.example.valoracion.internal.domain.MarcaPeliculaRepository;
import org.example.valoracion.internal.domain.ValoracionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Borra las valoraciones y marcas de una película cuando {@code pelicula} avisa de que se ha eliminado. */
@Component
public class LimpiezaPorPeliculaEliminada {

    private final ValoracionRepository valoraciones;
    private final MarcaPeliculaRepository marcas;

    public LimpiezaPorPeliculaEliminada(ValoracionRepository valoraciones, MarcaPeliculaRepository marcas) {
        this.valoraciones = valoraciones;
        this.marcas = marcas;
    }

    @EventListener
    @Transactional
    public void alEliminarPelicula(PeliculaEliminada evento) {
        valoraciones.deleteByPeliculaId(evento.peliculaId());
        marcas.deleteByPeliculaId(evento.peliculaId());
    }
}
