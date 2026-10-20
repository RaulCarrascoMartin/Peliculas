package org.example.pelicula.application;

import org.example.compartido.RepositorioEnMemoria;
import org.example.pelicula.domain.Pelicula;
import org.example.pelicula.domain.PeliculaRepository;

public class PeliculaRepositoryFalso extends RepositorioEnMemoria<Pelicula> implements PeliculaRepository {

    @Override
    protected Long idDe(Pelicula pelicula) {
        return pelicula.getId();
    }

    @Override
    protected void asignarId(Pelicula pelicula, Long id) {
        pelicula.setId(id);
    }
}
