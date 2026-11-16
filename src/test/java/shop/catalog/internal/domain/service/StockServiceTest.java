package shop.catalog.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.dto.StockItemDto;
import shop.catalog.internal.domain.exception.InsufficientStockException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.mapper.MovieMapper;
import shop.catalog.internal.domain.repository.MovieRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Stock rules with the repository mocked; the MapStruct mapper is the real one. */
class StockServiceTest {

    private static final long ALIEN = 1L;
    private static final long MATRIX = 2L;
    private static final long UNPRICED = 3L;

    private MovieRepository movies;
    private StockService service;

    @BeforeEach
    void setUp() {
        movies = mock(MovieRepository.class);
        service = new StockServiceImpl(movies, Mappers.getMapper(MovieMapper.class));
        when(movies.decrementStock(any(), anyInt())).thenReturn(1);
    }

    private static MovieDto movie(long id, String title, String price, int stock) {
        return new MovieDto(id, title, 1979, "Drama", "X", price == null ? null : new BigDecimal(price), null, stock);
    }

    private void catalogHas(Set<Long> requested, MovieDto... found) {
        when(movies.findAllById(requested)).thenReturn(List.of(found));
    }

    @Test
    void reserveDeductsEachMovieAndCapturesTitleAndPrice() {
        catalogHas(Set.of(ALIEN, MATRIX), movie(ALIEN, "Alien", "12.99", 5), movie(MATRIX, "Matrix", "7.50", 2));

        List<ReservationDto> reserved = service.reserve(
                List.of(new StockItemDto(ALIEN, 3), new StockItemDto(MATRIX, 2)));

        assertEquals(List.of(
                new ReservationDto(ALIEN, "Alien", new BigDecimal("12.99"), 3),
                new ReservationDto(MATRIX, "Matrix", new BigDecimal("7.50"), 2)), reserved);
        verify(movies).decrementStock(ALIEN, 3);
        verify(movies).decrementStock(MATRIX, 2);
        verify(movies, never()).incrementStock(any(), anyInt());
    }

    @Test
    void theWholeStockCanBeReserved() {
        catalogHas(Set.of(ALIEN), movie(ALIEN, "Alien", "12.99", 5));

        service.reserve(List.of(new StockItemDto(ALIEN, 5)));

        verify(movies).decrementStock(ALIEN, 5);
    }

    @Test
    void repeatedLinesAreSummedForTheDeductionButAnsweredOneByOne() {
        catalogHas(Set.of(ALIEN), movie(ALIEN, "Alien", "12.99", 5));

        var reserved = service.reserve(List.of(new StockItemDto(ALIEN, 2), new StockItemDto(ALIEN, 1)));

        assertEquals(2, reserved.size());
        assertEquals(2, reserved.get(0).quantity());
        assertEquals(1, reserved.get(1).quantity());
        verify(movies).decrementStock(ALIEN, 3);
    }

    @Test
    void repeatedLineIsSummedBeforeCheckingStock() {
        catalogHas(Set.of(ALIEN), movie(ALIEN, "Alien", "12.99", 3));

        var ex = assertThrows(InsufficientStockException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 2), new StockItemDto(ALIEN, 2))));

        assertEquals(ALIEN, ex.movieId());
        verify(movies, never()).decrementStock(any(), anyInt());
    }

    @Test
    void oneUnitMoreThanTheStockFailsAndTouchesNoStock() {
        catalogHas(Set.of(ALIEN, MATRIX), movie(ALIEN, "Alien", "12.99", 5), movie(MATRIX, "Matrix", "7.50", 1));

        var ex = assertThrows(InsufficientStockException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 2), new StockItemDto(MATRIX, 2))));

        assertEquals(MATRIX, ex.movieId());
        assertTrue(ex.getMessage().contains("2"));
        verify(movies, never()).decrementStock(any(), anyInt());
    }

    @Test
    void unknownMovieFailsAndTouchesNoStock() {
        catalogHas(Set.of(ALIEN, 99L), movie(ALIEN, "Alien", "12.99", 5));

        var ex = assertThrows(MovieNotFoundException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 1), new StockItemDto(99L, 1))));

        assertEquals(99L, ex.movieId());
        assertTrue(ex.getMessage().contains("99"));
        verify(movies, never()).decrementStock(any(), anyInt());
    }

    @Test
    void movieWithoutPriceCannotBeReserved() {
        catalogHas(Set.of(ALIEN, UNPRICED), movie(ALIEN, "Alien", "12.99", 5), movie(UNPRICED, "Silent", null, 5));

        var ex = assertThrows(MovieNotPurchasableException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 1), new StockItemDto(UNPRICED, 1))));

        assertEquals(UNPRICED, ex.movieId());
        assertTrue(ex.getMessage().contains("3"));
        verify(movies, never()).decrementStock(any(), anyInt());
    }

    @Test
    void deductionThatLosesARaceGivesBackWhatWasAlreadyDeducted() {
        catalogHas(Set.of(ALIEN, MATRIX), movie(ALIEN, "Alien", "12.99", 5), movie(MATRIX, "Matrix", "7.50", 2));
        when(movies.decrementStock(MATRIX, 2)).thenReturn(0);

        var ex = assertThrows(InsufficientStockException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 3), new StockItemDto(MATRIX, 2))));

        assertEquals(MATRIX, ex.movieId());
        InOrder order = inOrder(movies);
        order.verify(movies).decrementStock(ALIEN, 3);
        order.verify(movies).decrementStock(MATRIX, 2);
        order.verify(movies).incrementStock(ALIEN, 3);
        verify(movies, never()).incrementStock(MATRIX, 2);
    }

    @Test
    void firstDeductionThatLosesARaceHasNothingToGiveBack() {
        catalogHas(Set.of(ALIEN, MATRIX), movie(ALIEN, "Alien", "12.99", 5), movie(MATRIX, "Matrix", "7.50", 2));
        when(movies.decrementStock(ALIEN, 3)).thenReturn(0);

        assertThrows(InsufficientStockException.class,
                () -> service.reserve(List.of(new StockItemDto(ALIEN, 3), new StockItemDto(MATRIX, 2))));

        verify(movies, never()).decrementStock(MATRIX, 2);
        verify(movies, never()).incrementStock(any(), anyInt());
    }

    @Test
    void releaseGivesBackEveryLineAndIgnoresMoviesThatNoLongerExist() {
        when(movies.incrementStock(ALIEN, 3)).thenReturn(1);
        when(movies.incrementStock(99L, 1)).thenReturn(0);

        service.release(List.of(new StockItemDto(ALIEN, 3), new StockItemDto(99L, 1)));

        verify(movies).incrementStock(ALIEN, 3);
        verify(movies).incrementStock(99L, 1);
    }
}
