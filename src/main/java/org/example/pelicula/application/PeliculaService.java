package org.example.pelicula.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.PeliculaApi;
import org.example.pelicula.PeliculaEliminada;
import org.example.pelicula.PeliculaResumen;
import org.example.pelicula.domain.Pelicula;
import org.example.pelicula.domain.PeliculaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class PeliculaService implements PeliculaApi {

    private final PeliculaRepository peliculas;
    private final ApplicationEventPublisher eventos;

    public PeliculaService(PeliculaRepository peliculas, ApplicationEventPublisher eventos) {
        this.peliculas = peliculas;
        this.eventos = eventos;
    }

    public Pelicula crear(Pelicula pelicula) {
        return peliculas.save(pelicula);
    }

    public Pelicula detalle(Long id) {
        return peliculas.findById(id).orElseThrow(() -> noEncontrada(id));
    }

    /** Lista paginada; los filtros nulos o en blanco se ignoran y los indicados se combinan con "y". */
    public Page<Pelicula> buscar(String titulo, String genero, Integer anio, String director, Pageable paginacion) {
        return peliculas.buscar(texto(titulo), texto(genero), anio, texto(director), paginacion);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
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

    /** Borra la película y avisa con {@link PeliculaEliminada}; los oyentes actúan en la misma transacción. */
    @Transactional
    public void eliminar(Long id) {
        if (!peliculas.existsById(id)) {
            throw noEncontrada(id);
        }
        peliculas.deleteById(id);
        eventos.publishEvent(new PeliculaEliminada(id));
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

    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        return peliculas.findAllById(ids).stream()
                .sorted(Comparator.comparing(Pelicula::getId))
                .map(p -> new PeliculaResumen(p.getId(), p.getTitulo(), p.getAnio(), p.getGenero(),
                        p.getDirector(), p.getValoracionMedia()))
                .toList();
    }

    private static RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("Película " + id + " no encontrada");
    }
}
