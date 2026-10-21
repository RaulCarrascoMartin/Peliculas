package org.example.valoracion.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.compartido.UsuarioActual;
import org.example.pelicula.PeliculaApi;
import org.example.valoracion.domain.MarcaPelicula;
import org.example.valoracion.domain.MarcaPeliculaRepository;
import org.example.valoracion.domain.TipoMarca;
import org.springframework.stereotype.Service;

import java.util.List;

/** Listas personales de películas favoritas y vistas del usuario actual. */
@Service
public class MarcaPeliculaService {

    private final MarcaPeliculaRepository marcas;
    private final PeliculaApi peliculas;
    private final UsuarioActual usuarioActual;

    public MarcaPeliculaService(MarcaPeliculaRepository marcas, PeliculaApi peliculas, UsuarioActual usuarioActual) {
        this.marcas = marcas;
        this.peliculas = peliculas;
        this.usuarioActual = usuarioActual;
    }

    /** Añade la película a la lista; si ya estaba, no hace nada. */
    public void marcar(Long peliculaId, TipoMarca tipo) {
        if (!peliculas.existe(peliculaId)) {
            throw new RecursoNoEncontradoException("Película " + peliculaId + " no encontrada");
        }
        Long usuarioId = usuarioActual.id();
        if (marcas.findByUsuarioIdAndPeliculaIdAndTipo(usuarioId, peliculaId, tipo).isEmpty()) {
            MarcaPelicula marca = new MarcaPelicula();
            marca.setUsuarioId(usuarioId);
            marca.setPeliculaId(peliculaId);
            marca.setTipo(tipo);
            marcas.save(marca);
        }
    }

    /** Quita la película de la lista; si no estaba, no hace nada. */
    public void desmarcar(Long peliculaId, TipoMarca tipo) {
        marcas.findByUsuarioIdAndPeliculaIdAndTipo(usuarioActual.id(), peliculaId, tipo)
                .ifPresent(marcas::delete);
    }

    /** Ids de las películas de la lista del usuario actual. */
    public List<Long> peliculasMarcadas(TipoMarca tipo) {
        return marcas.findByUsuarioIdAndTipo(usuarioActual.id(), tipo).stream()
                .map(MarcaPelicula::getPeliculaId)
                .toList();
    }
}
