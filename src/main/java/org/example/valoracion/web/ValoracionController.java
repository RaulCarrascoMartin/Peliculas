package org.example.valoracion.web;

import jakarta.validation.Valid;
import org.example.valoracion.application.ValoracionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas/{peliculaId}/valoracion")
public class ValoracionController {

    private final ValoracionService servicio;

    public ValoracionController(ValoracionService servicio) {
        this.servicio = servicio;
    }

    @PutMapping
    public ValoracionResponse valorar(@PathVariable Long peliculaId, @Valid @RequestBody ValoracionRequest peticion) {
        return ValoracionResponse.desde(servicio.valorar(peliculaId, peticion.puntuacion(), peticion.comentario()));
    }
}
