package shop.orders.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.exception.InsufficientStockException;
import shop.orders.internal.domain.exception.ProductNotFoundException;
import shop.orders.internal.domain.exception.ProductNotPurchasableException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The adapter translates the catalog's facade (types and failures) to the contract defined by orders. */
class StockReservationAdapterTest {

    private static final List<StockItemDto> ITEMS = List.of(new StockItemDto(1L, 2));

    @Test
    void translatesReservedLines() {
        var adapter = new StockReservationAdapter(new CatalogStub(
                lines -> List.of(new ReservedLineDto(1L, "Alien", new BigDecimal("9.99"), 2))));

        var reserved = adapter.reserve(ITEMS);

        assertEquals(List.of(new ReservedItemDto(1L, "Alien", new BigDecimal("9.99"), 2)), reserved);
    }

    @Test
    void unknownProductBecomesTheDomainNotFound() {
        var adapter = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new UnknownProductException(1L);
        }));

        var ex = assertThrows(ProductNotFoundException.class, () -> adapter.reserve(ITEMS));
        assertEquals("El producto 1 no existe", ex.getMessage());
    }

    @Test
    void lackOfStockOrNoPriceBecomeTheDomainConflicts() {
        var noStock = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new shop.catalog.api.facade.exception.InsufficientStockException(1L);
        }));
        var noPrice = new StockReservationAdapter(new CatalogStub(lines -> {
            throw new shop.catalog.api.facade.exception.ProductNotPurchasableException(1L);
        }));

        assertThrows(InsufficientStockException.class, () -> noStock.reserve(ITEMS));
        assertThrows(ProductNotPurchasableException.class, () -> noPrice.reserve(ITEMS));
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
