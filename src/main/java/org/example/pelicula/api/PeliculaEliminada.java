package org.example.pelicula.api;

/** Evento que {@code pelicula} publica al borrar una película, para que otros módulos limpien sus datos. */
public record PeliculaEliminada(Long peliculaId) {
}
