package shop.catalog.internal.usecase;

import shop.compartido.RepositorioEnMemoria;
import shop.catalog.internal.domain.Pelicula;
import shop.catalog.internal.domain.PeliculaRepository;
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

    @Override
    public Page<Pelicula> buscar(String titulo, String genero, Integer anio, String director, Pageable paginacion) {
        List<Pelicula> filtradas = entidades.values().stream()
                .filter(p -> titulo == null || p.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                .filter(p -> genero == null || genero.equalsIgnoreCase(p.getGenero()))
                .filter(p -> anio == null || anio.equals(p.getAnio()))
                .filter(p -> director == null || director.equalsIgnoreCase(p.getDirector()))
                .toList();
        int desde = (int) Math.min(paginacion.getOffset(), filtradas.size());
        int hasta = Math.min(desde + paginacion.getPageSize(), filtradas.size());
        return new PageImpl<>(filtradas.subList(desde, hasta), paginacion, filtradas.size());
    }

    @Override
    public List<Pelicula> findAllById(Iterable<Long> ids) {
        List<Pelicula> encontradas = new java.util.ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(encontradas::add));
        return encontradas;
    }

    @Override
    public int descontarStock(Long id, int cantidad) {
        return findById(id).filter(p -> p.getStock() >= cantidad).map(p -> {
            p.setStock(p.getStock() - cantidad);
            return 1;
        }).orElse(0);
    }

    @Override
    public int aumentarStock(Long id, int cantidad) {
        return findById(id).map(p -> {
            p.setStock(p.getStock() + cantidad);
            return 1;
        }).orElse(0);
    }
}
