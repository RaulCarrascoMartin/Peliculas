package org.example.valoracion.internal.web;

import jakarta.validation.Valid;
import org.example.valoracion.internal.usecase.ValorarPelicula;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas/{peliculaId}/valoracion")
public class ValoracionController {

    private final ValorarPelicula valorar;

    public ValoracionController(ValorarPelicula valorar) {
        this.valorar = valorar;
    }

    @PutMapping
    public ValoracionResponse valorar(@PathVariable Long peliculaId, @Valid @RequestBody ValoracionRequest peticion) {
        return ValoracionResponse.desde(valorar.ejecutar(peliculaId, peticion.puntuacion(), peticion.comentario()));
    }
}
