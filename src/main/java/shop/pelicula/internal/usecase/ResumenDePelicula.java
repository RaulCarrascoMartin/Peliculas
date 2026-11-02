package shop.pelicula.internal.usecase;

import shop.pelicula.api.PeliculaResumen;
import shop.pelicula.internal.domain.Pelicula;

/** Convierte la entidad en el record que sale de los casos de uso: el modelo nunca cruza este límite. */
final class ResumenDePelicula {

    private ResumenDePelicula() {
    }

    static PeliculaResumen de(Pelicula p) {
        return new PeliculaResumen(p.getId(), p.getTitulo(), p.getAnio(), p.getGenero(), p.getDirector(),
                p.getPrecio(), p.getValoracionMedia());
    }
}
