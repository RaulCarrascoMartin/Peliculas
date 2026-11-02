package org.example.pelicula.internal.usecase;

import org.example.pelicula.api.PeliculaResumen;
import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

@Service
public class CrearPelicula {

    private final PeliculaRepository peliculas;

    public CrearPelicula(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public PeliculaResumen ejecutar(DatosPelicula datos) {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(datos.titulo());
        pelicula.setAnio(datos.anio());
        pelicula.setGenero(datos.genero());
        pelicula.setDirector(datos.director());
        pelicula.setPrecio(datos.precio());
        return ResumenDePelicula.de(peliculas.save(pelicula));
    }
}
