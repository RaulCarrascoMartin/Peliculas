package shop.catalog.internal.application.facade;

import shop.catalog.internal.application.mapper.MovieSummaryMapper;
import shop.catalog.internal.application.usecase.ReleaseStock;
import shop.catalog.internal.application.usecase.ReserveStock;

import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.internal.domain.service.MovieService;

import java.util.Collection;
import java.util.List;

/** Module exit point: implements {@link CatalogFacade} for the other modules. */
public class CatalogFacadeImpl implements CatalogFacade {

    private final MovieService movies;
    private final ReserveStock reserveStock;
    private final ReleaseStock releaseStock;

    public CatalogFacadeImpl(MovieService movies, ReserveStock reserveStock, ReleaseStock releaseStock) {
        this.movies = movies;
        this.reserveStock = reserveStock;
        this.releaseStock = releaseStock;
    }

    @Override
    public boolean exists(Long id) {
        return movies.exists(id);
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        movies.updateAverageRating(id, media);
    }

    @Override
    public List<MovieSummaryDto> findSummaries(Collection<Long> ids) {
        return movies.findAllById(ids).stream().map(MovieSummaryMapper::toSummary).toList();
    }

    @Override
    public List<ReservedLineDto> reserve(List<StockLineDto> lines) {
        return reserveStock.execute(lines);
    }

    @Override
    public void release(List<StockLineDto> lines) {
        releaseStock.execute(lines);
    }
}
