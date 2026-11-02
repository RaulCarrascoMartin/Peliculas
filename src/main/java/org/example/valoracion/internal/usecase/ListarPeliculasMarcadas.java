package org.example.valoracion.internal.usecase;

import org.example.compartido.UsuarioActual;
import org.example.pelicula.api.PeliculaApi;
import org.example.pelicula.api.PeliculaResumen;
import org.example.valoracion.internal.domain.MarcaPelicula;
import org.example.valoracion.internal.domain.MarcaPeliculaRepository;
import org.example.valoracion.internal.domain.TipoMarca;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarPeliculasMarcadas {

    private final MarcaPeliculaRepository marcas;
    private final PeliculaApi peliculas;
    private final UsuarioActual usuarioActual;

    public ListarPeliculasMarcadas(MarcaPeliculaRepository marcas, PeliculaApi peliculas,
                                   UsuarioActual usuarioActual) {
        this.marcas = marcas;
        this.peliculas = peliculas;
        this.usuarioActual = usuarioActual;
    }

    /** Películas de la lista del usuario actual, ordenadas por id. */
    public List<PeliculaResumen> ejecutar(TipoMarca tipo) {
        List<Long> ids = marcas.findByUsuarioIdAndTipo(usuarioActual.id(), tipo).stream()
                .map(MarcaPelicula::getPeliculaId)
                .toList();
        return peliculas.resumenes(ids);
    }
}
