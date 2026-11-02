package shop.valoracion.internal.usecase;

import shop.compartido.error.RecursoNoEncontradoException;
import shop.compartido.seguridad.UsuarioActual;
import shop.catalog.api.CatalogApi;
import shop.valoracion.internal.domain.MarcaPelicula;
import shop.valoracion.internal.domain.MarcaPeliculaRepository;
import shop.valoracion.internal.domain.TipoMarca;
import org.springframework.stereotype.Service;

@Service
public class MarcarPelicula {

    private final MarcaPeliculaRepository marcas;
    private final CatalogApi peliculas;
    private final UsuarioActual usuarioActual;

    public MarcarPelicula(MarcaPeliculaRepository marcas, CatalogApi peliculas, UsuarioActual usuarioActual) {
        this.marcas = marcas;
        this.peliculas = peliculas;
        this.usuarioActual = usuarioActual;
    }

    /** Añade la película a la lista; si ya estaba, no hace nada. */
    public void ejecutar(Long peliculaId, TipoMarca tipo) {
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
}
