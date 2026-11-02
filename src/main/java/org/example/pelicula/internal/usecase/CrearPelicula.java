package org.example.pelicula.internal.usecase;

import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class CrearPelicula {

    private final PeliculaRepository peliculas;

    public CrearPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public Pelicula ejecutar(Pelicula pelicula) {
        return peliculas.save(pelicula);
    }
}
