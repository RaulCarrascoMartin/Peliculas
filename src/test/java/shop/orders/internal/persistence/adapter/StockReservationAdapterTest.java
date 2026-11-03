package shop.orders.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.orders.internal.domain.contract.StockReservation;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The adapter translates the catalog's facade (types and failures) to the contract defined by orders. */
class StockReservationAdapterTest {

    private static final List<StockReservation.Item> ITEMS = List.of(new StockReservation.Item(1L, 2));

    @Test
    void translatesReservedLines() {
        var adapter = new StockReservationAdapter(new CatalogStub(
                lines -> List.of(new ReservedLineDto(1L, "Alien", new BigDecimal("9.99"), 2))));

        var reserved = adapter.reserve(ITEMS);

        assertEquals(List.of(new StockReservation.Reserved(1L, "Alien", new BigDecimal("9.99"), 2)), reserved);
    }

    @Test
    void unknownProductBecomesNotFound() {
        var adapter = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new UnknownProductException(1L);
        }));

        assertThrows(ResourceNotFoundException.class, () -> adapter.reserve(ITEMS));
    }

    @Test
    void lackOfStockOrNoPriceBecomesConflict() {
        var noStock = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new InsufficientStockException(1L);
        }));
        var noPrice = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new ProductNotPurchasableException(1L);
        }));

        assertThrows(ConflictException.class, () -> noStock.reserve(ITEMS));
        assertThrows(ConflictException.class, () -> noPrice.reserve(ITEMS));
    }

    private record CatalogStub(java.util.function.Function<List<StockLineDto>, List<ReservedLineDto>> onReserve)
            implements CatalogFacade {

        @Override
        public List<ReservedLineDto> reserve(List<StockLineDto> lines) {
            return onReserve.apply(lines);
        }

        @Override
        public void release(List<StockLineDto> lines) {
        }

        @Override
        public boolean exists(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAverageRating(Long id, Double media) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MovieSummaryDto> findSummaries(Collection<Long> ids) {
            throw new UnsupportedOperationException();
        }
    }
}
