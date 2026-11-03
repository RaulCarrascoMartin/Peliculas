package shop.catalog.internal;

import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.internal.usecase.ReleaseStock;
import shop.catalog.internal.usecase.ReserveStock;
import shop.catalog.api.MovieSummary;
import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Module exit point: implements {@link CatalogApi} for the other modules. */
@Service
public class CatalogApiImpl implements CatalogApi {

    private final MovieRepository movies;
    private final ReserveStock reserveStock;
    private final ReleaseStock releaseStock;

    public CatalogApiImpl(MovieRepository movies, ReserveStock reserveStock, ReleaseStock releaseStock) {
        this.movies = movies;
        this.reserveStock = reserveStock;
        this.releaseStock = releaseStock;
    }

    @Override
    public boolean exists(Long id) {
        return movies.existsById(id);
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        Movie movie = movies.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película " + id + " no encontrada"));
        movie.setAverageRating(media);
        movies.save(movie);
    }

    @Override
    public List<MovieSummary> findSummaries(Collection<Long> ids) {
        return movies.findAllById(ids).stream()
                .sorted(Comparator.comparing(Movie::getId))
                .map(p -> new MovieSummary(p.getId(), p.getTitle(), p.getYear(), p.getGenre(),
                        p.getDirector(), p.getPrice(), p.getAverageRating(), p.getStock()))
                .toList();
    }

    @Override
    public List<ReservedLine> reserve(List<StockLine> lines) {
        return reserveStock.execute(lines);
    }

    @Override
    public void release(List<StockLine> lines) {
        releaseStock.execute(lines);
    }
}
