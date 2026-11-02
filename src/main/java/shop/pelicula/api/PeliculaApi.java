package shop.pelicula.api;

import java.util.Collection;
import java.util.List;

/** API pública del módulo {@code pelicula}: lo único que otros módulos pueden usar. */
public interface PeliculaApi {

    boolean existe(Long id);

    /** Guarda la media calculada por otro módulo; lanza 404 si la película no existe. */
    void actualizarValoracionMedia(Long id, Double media);

    /** Datos de las películas pedidas, ordenadas por id; los ids inexistentes se ignoran. */
    List<PeliculaResumen> resumenes(Collection<Long> ids);
}
