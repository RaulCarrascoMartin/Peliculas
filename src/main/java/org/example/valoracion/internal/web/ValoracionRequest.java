package org.example.valoracion.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ValoracionRequest(
        @NotNull @Min(1) @Max(5) Integer puntuacion,
        String comentario) {
}
