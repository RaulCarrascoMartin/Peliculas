package shop.catalog.internal.application.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.dto.StockItemDto;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.service.MovieService;
import shop.catalog.internal.domain.service.StockService;
import shop.shared.error.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The facade only translates: public types ↔ domain DTO, and domain exceptions → facade exceptions. */
class CatalogFacadeImplTest {

    private static final List<StockLineDto> LINES = List.of(new StockLineDto(1L, 3), new StockLineDto(2L, 2));
    private static final List<StockItemDto> ITEMS = List.of(new StockItemDto(1L, 3), new StockItemDto(2L, 2));

    private MovieService movies;
    private StockService stock;
    private CatalogFacadeImpl facade;

    @BeforeEach
    void setUp() {
        movies = mock(MovieService.class);
        stock = mock(StockService.class);
        facade = new CatalogFacadeImpl(movies, stock);
    }

    @Test
    void existsAsksTheService() {
        when(movies.exists(1L)).thenReturn(true);

        assertTrue(facade.exists(1L));
        assertFalse(facade.exists(99L));
    }

    @Test
    void updateAverageRatingDelegatesAndLetsTheNotFoundThrough() {
        facade.updateAverageRating(1L, 4.5);
        verify(movies).updateAverageRating(1L, 4.5);

        doThrow(new MovieNotFoundException(99L)).when(movies).updateAverageRating(99L, 3.5);
        assertThrows(ResourceNotFoundException.class, () -> facade.updateAverageRating(99L, 3.5));
    }

    @Test
    void findSummariesMapsEveryFieldKeepingTheOrderOfTheService() {
        when(movies.findAllById(List.of(2L, 1L, 99L))).thenReturn(List.of(
                new MovieDto(1L, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), null, 3),
                new MovieDto(2L, "Gladiator", 2000, "Drama", "Ridley Scott", null, 4.5, 0)));

        assertEquals(List.of(
                        new MovieSummaryDto(1L, "Alien", 1979, "Science fiction", "Ridley Scott",
                                new BigDecimal("12.99"), null, 3),
                        new MovieSummaryDto(2L, "Gladiator", 2000, "Drama", "Ridley Scott", null, 4.5, 0)),
                facade.findSummaries(List.of(2L, 1L, 99L)));
    }

    @Test
    void findSummariesWithoutIdsReturnsEmptyList() {
        when(movies.findAllById(List.of())).thenReturn(List.of());

        assertTrue(facade.findSummaries(List.of()).isEmpty());
    }

    @Test
    void reserveTranslatesLinesAndReservations() {
        when(stock.reserve(ITEMS)).thenReturn(List.of(
                new ReservationDto(1L, "Alien", new BigDecimal("12.99"), 3),
                new ReservationDto(2L, "Matrix", new BigDecimal("7.50"), 2)));

        assertEquals(List.of(
                new ReservedLineDto(1L, "Alien", new BigDecimal("12.99"), 3),
                new ReservedLineDto(2L, "Matrix", new BigDecimal("7.50"), 2)), facade.reserve(LINES));
    }

    @Test
    void unknownMovieLeavesTheModuleAsUnknownProduct() {
        when(stock.reserve(ITEMS)).thenThrow(new MovieNotFoundException(2L));

        var ex = assertThrows(UnknownProductException.class, () -> facade.reserve(LINES));

        assertEquals(2L, ex.productId());
    }

    @Test
    void movieWithoutPriceLeavesTheModuleAsProductNotPurchasable() {
        when(stock.reserve(ITEMS)).thenThrow(new MovieNotPurchasableException(2L));

        var ex = assertThrows(ProductNotPurchasableException.class, () -> facade.reserve(LINES));

        assertEquals(2L, ex.productId());
    }

    @Test
    void insufficientStockLeavesTheModuleAsTheFacadeException() {
        when(stock.reserve(ITEMS))
                .thenThrow(new shop.catalog.internal.domain.exception.InsufficientStockException(1L));

        var ex = assertThrows(InsufficientStockException.class, () -> facade.reserve(LINES));

        assertEquals(1L, ex.productId());
    }

    @Test
    void releaseTranslatesTheLines() {
        facade.release(LINES);

        verify(stock).release(ITEMS);
    }
}
