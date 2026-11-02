package org.example.pelicula.internal.usecase;

import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class EditarPelicula {

    private final PeliculaRepository peliculas;
    private final ObtenerPelicula obtener;

    public EditarPelicula(PeliculaRepository peliculas, ObtenerPelicula obtener) {
        this.peliculas = peliculas;
        this.obtener = obtener;
    }

    /** Sustituye título, año, género, director y precio; la valoración media no se edita a mano. */
    public Pelicula ejecutar(Long id, Pelicula datos) {
        Pelicula existente = obtener.ejecutar(id);
        existente.setTitulo(datos.getTitulo());
        existente.setAnio(datos.getAnio());
        existente.setGenero(datos.getGenero());
        existente.setDirector(datos.getDirector());
        existente.setPrecio(datos.getPrecio());
        return peliculas.save(existente);
    }
}
