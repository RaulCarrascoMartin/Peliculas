package org.example.pelicula.application;

import org.example.compartido.RepositorioEnMemoria;
import org.example.pelicula.domain.Pelicula;
import org.example.pelicula.domain.PeliculaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PeliculaRepositoryFalso extends RepositorioEnMemoria<Pelicula> implements PeliculaRepository {

    @Override
    protected Long idDe(Pelicula pelicula) {
        return pelicula.getId();
    }

    @Override
    protected void asignarId(Pelicula pelicula, Long id) {
        pelicula.setId(id);
    }

    @Override
    public Page<Pelicula> findAll(Pageable paginacion) {
        List<Pelicula> todas = List.copyOf(entidades.values());
        int desde = (int) Math.min(paginacion.getOffset(), todas.size());
        int hasta = Math.min(desde + paginacion.getPageSize(), todas.size());
        return new PageImpl<>(todas.subList(desde, hasta), paginacion, todas.size());
    }
}
