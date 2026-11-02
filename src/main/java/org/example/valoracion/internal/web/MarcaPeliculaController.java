package org.example.valoracion.internal.web;

import org.example.valoracion.internal.usecase.DesmarcarPelicula;
import org.example.valoracion.internal.usecase.ListarPeliculasMarcadas;
import org.example.valoracion.internal.usecase.MarcarPelicula;
import org.example.valoracion.internal.domain.TipoMarca;
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

    private final MarcarPelicula marcar;
    private final DesmarcarPelicula desmarcar;
    private final ListarPeliculasMarcadas listar;

    public MarcaPeliculaController(MarcarPelicula marcar, DesmarcarPelicula desmarcar,
                                   ListarPeliculasMarcadas listar) {
        this.marcar = marcar;
        this.desmarcar = desmarcar;
        this.listar = listar;
    }

    @PutMapping("/api/peliculas/{peliculaId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarFavorita(@PathVariable Long peliculaId) {
        marcar.ejecutar(peliculaId, TipoMarca.FAVORITA);
    }

    @DeleteMapping("/api/peliculas/{peliculaId}/favorita")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcarFavorita(@PathVariable Long peliculaId) {
        desmarcar.ejecutar(peliculaId, TipoMarca.FAVORITA);
    }

    @PutMapping("/api/peliculas/{peliculaId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarVista(@PathVariable Long peliculaId) {
        marcar.ejecutar(peliculaId, TipoMarca.VISTA);
    }

    @DeleteMapping("/api/peliculas/{peliculaId}/vista")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcarVista(@PathVariable Long peliculaId) {
        desmarcar.ejecutar(peliculaId, TipoMarca.VISTA);
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
        return listar.ejecutar(tipo).stream().map(PeliculaMarcadaResponse::desde).toList();
    }
}
