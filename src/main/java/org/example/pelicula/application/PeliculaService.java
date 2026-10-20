package org.example.pelicula.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.PeliculaApi;
import org.example.pelicula.domain.Pelicula;
import org.example.pelicula.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class PeliculaService implements PeliculaApi {

    private final PeliculaRepository peliculas;

    public PeliculaService(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public Pelicula crear(Pelicula pelicula) {
        return peliculas.save(pelicula);
    }

    public Pelicula detalle(Long id) {
        return peliculas.findById(id).orElseThrow(() -> noEncontrada(id));
    }

    /** Sustituye título, año, género y director; la valoración media no se edita a mano. */
    public Pelicula editar(Long id, Pelicula datos) {
        Pelicula existente = detalle(id);
        existente.setTitulo(datos.getTitulo());
        existente.setAnio(datos.getAnio());
        existente.setGenero(datos.getGenero());
        existente.setDirector(datos.getDirector());
        return peliculas.save(existente);
    }

    public void eliminar(Long id) {
        if (!peliculas.existsById(id)) {
            throw noEncontrada(id);
        }
        peliculas.deleteById(id);
    }

    @Override
    public boolean existe(Long id) {
        return peliculas.existsById(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        Pelicula pelicula = detalle(id);
        pelicula.setValoracionMedia(media);
        peliculas.save(pelicula);
    }

    private static RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("Película " + id + " no encontrada");
    }
}
