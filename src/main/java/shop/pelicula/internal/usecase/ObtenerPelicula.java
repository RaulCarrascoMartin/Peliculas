package shop.pelicula.internal.usecase;

import shop.compartido.error.RecursoNoEncontradoException;
import shop.pelicula.api.PeliculaResumen;
import shop.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class ObtenerPelicula {

    private final PeliculaRepository peliculas;

    public ObtenerPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public PeliculaResumen ejecutar(Long id) {
        return peliculas.findById(id)
                .map(ResumenDePelicula::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película " + id + " no encontrada"));
    }
}
