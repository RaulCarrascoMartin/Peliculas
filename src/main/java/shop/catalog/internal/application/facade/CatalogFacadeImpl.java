package shop.catalog.internal.application.facade;

import org.springframework.stereotype.Component;
import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.dto.StockItemDto;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.service.MovieService;
import shop.catalog.internal.domain.service.StockService;

import java.util.Collection;
import java.util.List;

/**
 * Module exit point: implements {@link CatalogFacade} for the other modules, the only bean they reach. It only
 * translates between the public types of the facade and the DTO and exceptions of the domain.
 */
@Component
public class CatalogFacadeImpl implements CatalogFacade {

    private final MovieService movies;
    private final StockService stock;

    public CatalogFacadeImpl(MovieService movies, StockService stock) {
        this.movies = movies;
        this.stock = stock;
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
        return movies.findAllById(ids).stream().map(CatalogFacadeImpl::toSummary).toList();
    }

    /** Reserves all the lines or none; the domain failures leave the module as the facade's exceptions. */
    @Override
    public List<ReservedLineDto> reserve(List<StockLineDto> lines) {
        try {
            return stock.reserve(toItems(lines)).stream()
                    .map(r -> new ReservedLineDto(r.movieId(), r.title(), r.unitPrice(), r.quantity()))
                    .toList();
        } catch (MovieNotFoundException e) {
            throw new UnknownProductException(e.movieId());
        } catch (MovieNotPurchasableException e) {
            throw new ProductNotPurchasableException(e.movieId());
        } catch (shop.catalog.internal.domain.exception.InsufficientStockException e) {
            throw new InsufficientStockException(e.movieId());
        }
    }

    /** Returns the units to the stock; a product that no longer exists is ignored. */
    @Override
    public void release(List<StockLineDto> lines) {
        stock.release(toItems(lines));
    }

    private static List<StockItemDto> toItems(List<StockLineDto> lines) {
        return lines.stream().map(l -> new StockItemDto(l.productId(), l.quantity())).toList();
    }

    private static MovieSummaryDto toSummary(MovieDto movie) {
        return new MovieSummaryDto(movie.id(), movie.title(), movie.year(), movie.genre(), movie.director(),
                movie.price(), movie.averageRating(), movie.stock());
    }
}
