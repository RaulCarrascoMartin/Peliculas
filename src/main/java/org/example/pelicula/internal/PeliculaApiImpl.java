package org.example.pelicula.internal;

import org.example.pelicula.api.PeliculaApi;
import org.example.pelicula.api.PeliculaResumen;
import org.example.pelicula.internal.domain.Pelicula;
import org.example.pelicula.internal.domain.PeliculaRepository;
import org.example.pelicula.internal.usecase.ObtenerPelicula;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Punto de salida del módulo: implementa {@link PeliculaApi} para los demás módulos. */
@Service
public class PeliculaApiImpl implements PeliculaApi {

    private final PeliculaRepository peliculas;
    private final ObtenerPelicula obtener;

    public PeliculaApiImpl(PeliculaRepository peliculas, ObtenerPelicula obtener) {
        this.peliculas = peliculas;
        this.obtener = obtener;
    }

    @Override
    public boolean existe(Long id) {
        return peliculas.existsById(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        Pelicula pelicula = obtener.ejecutar(id);
        pelicula.setValoracionMedia(media);
        peliculas.save(pelicula);
    }

    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        return peliculas.findAllById(ids).stream()
                .sorted(Comparator.comparing(Pelicula::getId))
                .map(p -> new PeliculaResumen(p.getId(), p.getTitulo(), p.getAnio(), p.getGenero(),
                        p.getDirector(), p.getPrecio(), p.getValoracionMedia()))
                .toList();
    }
}
