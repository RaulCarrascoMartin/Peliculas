package shop.shared;

import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Hand-written {@link CatalogFacade} for the adapter tests: one fixed movie, and a record of what was called. */
public class CatalogFacadeStub implements CatalogFacade {

    public static final MovieSummaryDto ALIEN =
            new MovieSummaryDto(1L, "Alien", 1979, "Horror", "Ridley Scott", new BigDecimal("9.99"), 4.5, 3);

    public final List<String> calls = new ArrayList<>();

    @Override
    public boolean exists(Long id) {
        return id.equals(ALIEN.id());
    }

    @Override
    public void updateAverageRating(Long id, Double media) {
        calls.add("updateAverageRating:" + id + ":" + media);
    }

    @Override
    public List<MovieSummaryDto> findSummaries(Collection<Long> ids) {
        return ids.contains(ALIEN.id()) ? List.of(ALIEN) : List.of();
    }

    @Override
    public List<ReservedLineDto> reserve(List<StockLineDto> lines) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release(List<StockLineDto> lines) {
        throw new UnsupportedOperationException();
    }
}
