package org.example.valoracion.web;

import org.example.valoracion.domain.Valoracion;

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
