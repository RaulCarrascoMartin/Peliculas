package org.example.valoracion.internal.web;

import org.example.valoracion.internal.usecase.ValoracionVista;

public record ValoracionResponse(
        Long id,
        Long peliculaId,
        Integer puntuacion,
        String comentario) {

    public static ValoracionResponse desde(ValoracionVista v) {
        return new ValoracionResponse(v.id(), v.peliculaId(), v.puntuacion(), v.comentario());
    }
}
