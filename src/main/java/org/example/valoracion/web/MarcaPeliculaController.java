package org.example.valoracion.web;

import org.example.valoracion.application.MarcaPeliculaService;
import org.example.valoracion.domain.TipoMarca;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MarcaPeliculaController {

    private final MarcaPeliculaService servicio;

    public MarcaPeliculaController(MarcaPeliculaService servicio) {
        this.servicio = servicio;
    }

    @PutMapping("/api/peliculas/{peliculaId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarFavorita(@PathVariable Long peliculaId) {
        servicio.marcar(peliculaId, TipoMarca.FAVORITA);
    }

    @DeleteMapping("/api/peliculas/{peliculaId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcarFavorita(@PathVariable Long peliculaId) {
        servicio.desmarcar(peliculaId, TipoMarca.FAVORITA);
    }

    @PutMapping("/api/peliculas/{peliculaId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarVista(@PathVariable Long peliculaId) {
        servicio.marcar(peliculaId, TipoMarca.VISTA);
    }

    @DeleteMapping("/api/peliculas/{peliculaId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcarVista(@PathVariable Long peliculaId) {
        servicio.desmarcar(peliculaId, TipoMarca.VISTA);
    }

    @GetMapping("/api/usuarios/me/favoritas")
    public List<PeliculaMarcadaResponse> favoritas() {
        return lista(TipoMarca.FAVORITA);
    }

    @GetMapping("/api/usuarios/me/vistas")
    public List<PeliculaMarcadaResponse> vistas() {
        return lista(TipoMarca.VISTA);
    }

    private List<PeliculaMarcadaResponse> lista(TipoMarca tipo) {
        return servicio.peliculasMarcadas(tipo).stream().map(PeliculaMarcadaResponse::desde).toList();
    }
}
