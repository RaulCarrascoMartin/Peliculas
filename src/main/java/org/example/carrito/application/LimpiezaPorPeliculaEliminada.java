package org.example.carrito.application;

import org.example.carrito.domain.LineaCarritoRepository;
import org.example.pelicula.PeliculaEliminada;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borra las líneas de una película de todos los carritos cuando {@code pelicula} avisa de que se ha eliminado.
 * El nombre de bean va explícito: {@code valoracion} tiene otra {@code LimpiezaPorPeliculaEliminada}
 * (patrón idéntico) y los dos no pueden colisionar en el contexto.
 */
@Component("limpiezaCarritoPorPeliculaEliminada")
public class LimpiezaPorPeliculaEliminada {

    private final LineaCarritoRepository lineas;

    public LimpiezaPorPeliculaEliminada(LineaCarritoRepository lineas) {
        this.lineas = lineas;
    }

    @EventListener
    @Transactional
    public void alEliminarPelicula(PeliculaEliminada evento) {
        lineas.deleteByPeliculaId(evento.peliculaId());
    }
}
