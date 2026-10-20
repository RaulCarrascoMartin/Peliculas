package org.example.pelicula.web;

import jakarta.validation.Valid;
import org.example.pelicula.application.PeliculaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaController {

    private static final String SOLO_ADMIN = "hasRole('ADMIN')";

    private final PeliculaService servicio;

    public PeliculaController(PeliculaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    public PeliculaResponse detalle(@PathVariable Long id) {
        return PeliculaResponse.desde(servicio.detalle(id));
    }

    @PostMapping
    @PreAuthorize(SOLO_ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public PeliculaResponse crear(@Valid @RequestBody PeliculaRequest peticion) {
        return PeliculaResponse.desde(servicio.crear(peticion.aEntidad()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    public PeliculaResponse editar(@PathVariable Long id, @Valid @RequestBody PeliculaRequest peticion) {
        return PeliculaResponse.desde(servicio.editar(id, peticion.aEntidad()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
