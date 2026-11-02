package org.example.valoracion.internal.web;

import org.example.valoracion.internal.domain.Valoracion;

public record ValoracionResponse(
        Long id,
        Long peliculaId,
        Integer puntuacion,
        String comentario) {

    public static ValoracionResponse desde(Valoracion valoracion) {
        return new ValoracionResponse(valoracion.getId(), valoracion.getPeliculaId(),
                valoracion.getPuntuacion(), valoracion.getComentario());
    }
}
