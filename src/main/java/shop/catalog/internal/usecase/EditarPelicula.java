package shop.catalog.internal.usecase;

import shop.compartido.error.RecursoNoEncontradoException;
import shop.catalog.api.PeliculaResumen;
import shop.catalog.internal.domain.Pelicula;
import shop.catalog.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class EditarPelicula {

    private final PeliculaRepository peliculas;

    public EditarPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    /** Sustituye título, año, género, director y precio; la valoración media no se edita a mano. */
    public PeliculaResumen ejecutar(Long id, DatosPelicula datos) {
        Pelicula existente = peliculas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película " + id + " no encontrada"));
        existente.setTitulo(datos.titulo());
        existente.setAnio(datos.anio());
        existente.setGenero(datos.genero());
        existente.setDirector(datos.director());
        existente.setPrecio(datos.precio());
        return ResumenDePelicula.de(peliculas.save(existente));
    }
}
