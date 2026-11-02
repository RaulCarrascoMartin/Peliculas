package shop.catalog.internal;

import shop.compartido.error.RecursoNoEncontradoException;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.PeliculaResumen;
import shop.catalog.internal.domain.Pelicula;
import shop.catalog.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Punto de salida del módulo: implementa {@link CatalogApi} para los demás módulos. */
@Service
public class CatalogApiImpl implements CatalogApi {

    private final PeliculaRepository peliculas;

    public CatalogApiImpl(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    @Override
    public boolean existe(Long id) {
        return peliculas.existsById(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        Pelicula pelicula = peliculas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película " + id + " no encontrada"));
        pelicula.setValoracionMedia(media);
        peliculas.save(pelicula);
    }

    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        return peliculas.findAllById(ids).stream()
                .sorted(Comparator.comparing(Pelicula::getId))
                .map(p -> new PeliculaResumen(p.getId(), p.getTitulo(), p.getAnio(), p.getGenero(),
                        p.getDirector(), p.getPrecio(), p.getValoracionMedia()))
                .toList();
    }
}
