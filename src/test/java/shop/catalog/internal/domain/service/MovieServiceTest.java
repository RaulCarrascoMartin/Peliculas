package shop.catalog.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.InvalidSortPropertyException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.mapper.MovieMapper;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Movie rules with the repository mocked; the MapStruct mapper is the real one. */
class MovieServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("12.99");
    private static final PageQuery PAGE = PageQuery.of(0, 10);

    private MovieRepository movies;
    private MovieService service;

    @BeforeEach
    void setUp() {
        movies = mock(MovieRepository.class);
        service = new MovieServiceImpl(movies, Mappers.getMapper(MovieMapper.class));
    }

    private static MovieDetailsDto details(String title, Integer stock) {
        return new MovieDetailsDto(title, 1979, "Science fiction", "Ridley Scott", PRICE, stock);
    }

    /** Movie already stored, with an average rating and 5 units. */
    private static MovieDto stored(Long id) {
        return new MovieDto(id, "Alien", 1979, "Science fiction", "Ridley Scott", PRICE, 4.5, 5);
    }

    private void repositoryAssignsId(long id) {
        when(movies.save(any())).thenAnswer(call -> {
            MovieDto m = call.getArgument(0);
            return new MovieDto(id, m.title(), m.year(), m.genre(), m.director(), m.price(), m.averageRating(),
                    m.stock());
        });
    }

    // ---------------------------------------------------------------- create

    @Test
    void createSavesEveryEditableFieldWithoutIdNorAverageAndReturnsTheAssignedId() {
        repositoryAssignsId(10L);

        MovieDto created = service.create(details("Alien", 3));

        verify(movies).save(new MovieDto(null, "Alien", 1979, "Science fiction", "Ridley Scott", PRICE, null, 3));
        assertEquals(new MovieDto(10L, "Alien", 1979, "Science fiction", "Ridley Scott", PRICE, null, 3), created);
    }

    @Test
    void createWithoutStockStartsWithZeroUnits() {
        repositoryAssignsId(10L);

        assertEquals(0, service.create(details("Alien", null)).stock());
        assertEquals(0, service.create(details("Alien", 0)).stock());
    }

    @Test
    void createKeepsAMovieWithoutPriceAsItIs() {
        repositoryAssignsId(10L);

        MovieDto created = service.create(new MovieDetailsDto("Silent", 1950, "Drama", "Somebody", null, null));

        assertEquals(null, created.price());
    }

    // ---------------------------------------------------------------- update

    @Test
    void updateReplacesTheEditableDataAndKeepsIdAndAverageRating() {
        when(movies.findById(10L)).thenReturn(Optional.of(stored(10L)));
        when(movies.save(any())).thenAnswer(call -> call.getArgument(0));

        MovieDto updated = service.update(10L,
                new MovieDetailsDto("Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"), 7));

        MovieDto expected = new MovieDto(10L, "Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"),
                4.5, 7);
        verify(movies).save(expected);
        assertEquals(expected, updated);
    }

    @Test
    void updateWithoutStockLeavesTheStockUntouchedAndZeroIsAValue() {
        when(movies.findById(10L)).thenReturn(Optional.of(stored(10L)));
        when(movies.save(any())).thenAnswer(call -> call.getArgument(0));

        assertEquals(5, service.update(10L, details("Alien 3", null)).stock());
        assertEquals(0, service.update(10L, details("Alien 3", 0)).stock());
    }

    @Test
    void updatingUnknownMovieFailsAndSavesNothing() {
        when(movies.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(MovieNotFoundException.class, () -> service.update(99L, details("X", 1)));

        assertTrue(ex.getMessage().contains("99"));
        assertEquals(99L, ex.movieId());
        verify(movies, never()).save(any());
    }

    // ---------------------------------------------------------------- find, exists, delete

    @Test
    void findReturnsTheStoredMovie() {
        when(movies.findById(10L)).thenReturn(Optional.of(stored(10L)));

        assertEquals(stored(10L), service.find(10L));
    }

    @Test
    void findingUnknownMovieFails() {
        when(movies.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(MovieNotFoundException.class, () -> service.find(99L));

        assertEquals("Película 99 no encontrada", ex.getMessage());
    }

    @Test
    void existsAnswersWhatTheRepositorySays() {
        when(movies.existsById(10L)).thenReturn(true);

        assertTrue(service.exists(10L));
        assertFalse(service.exists(99L));
    }

    @Test
    void deleteRemovesAnExistingMovie() {
        when(movies.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(movies).deleteById(10L);
    }

    @Test
    void deletingUnknownMovieFailsAndDeletesNothing() {
        when(movies.existsById(99L)).thenReturn(false);

        var ex = assertThrows(MovieNotFoundException.class, () -> service.delete(99L));

        assertTrue(ex.getMessage().contains("99"));
        verify(movies, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- average rating

    @Test
    void updateAverageRatingChangesOnlyTheAverage() {
        when(movies.updateAverageRating(10L, 3.5)).thenReturn(1);

        service.updateAverageRating(10L, 3.5);

        verify(movies).updateAverageRating(10L, 3.5);
        verify(movies, never()).save(any());
    }

    @Test
    void averageRatingCanGoBackToNoRatings() {
        when(movies.updateAverageRating(10L, null)).thenReturn(1);

        service.updateAverageRating(10L, null);

        verify(movies).updateAverageRating(10L, null);
    }

    @Test
    void updatingAverageRatingOfUnknownMovieFails() {
        when(movies.updateAverageRating(99L, 3.5)).thenReturn(0);

        assertThrows(MovieNotFoundException.class, () -> service.updateAverageRating(99L, 3.5));

        verify(movies, never()).save(any());
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchPassesTheFiltersAndThePageAndReturnsThePageOfTheRepository() {
        var page = new PageResult<>(List.of(stored(1L), stored(2L)), 5, PageQuery.of(1, 2));
        when(movies.search("alien", "Drama", 1979, "Ridley Scott", PageQuery.of(1, 2))).thenReturn(page);

        var result = service.search("alien", "Drama", 1979, "Ridley Scott", PageQuery.of(1, 2));

        assertSame(page, result);
        assertEquals(5, result.total());
        assertEquals(3, result.totalPages());
    }

    @Test
    void searchIgnoresBlankFiltersAndStripsTheGivenOnes() {
        service.search("  ", "", null, " ", PAGE);
        verify(movies).search(null, null, null, null, PAGE);

        service.search(" alien ", " Drama", 1979, "Ridley Scott ", PAGE);
        verify(movies).search("alien", "Drama", 1979, "Ridley Scott", PAGE);
    }

    @Test
    void searchAcceptsEverySortableProperty() {
        for (String property : List.of("id", "title", "year", "genre", "director", "averageRating", "price", "stock")) {
            var page = new PageQuery(0, 10, List.of(new PageQuery.Sort(property, false)));

            assertDoesNotThrow(() -> service.search(null, null, null, null, page));
            verify(movies).search(null, null, null, null, page);
        }
    }

    @Test
    void searchRejectsAnUnknownSortPropertyBeforeReachingTheRepository() {
        var page = new PageQuery(0, 10, List.of(
                new PageQuery.Sort("title", true), new PageQuery.Sort("titulo; drop table peliculas", true)));

        var ex = assertThrows(InvalidSortPropertyException.class, () -> service.search(null, null, null, null, page));

        assertEquals("Propiedad de ordenación no válida: titulo; drop table peliculas", ex.getMessage());
        verify(movies, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    void sortPropertiesAreCaseSensitive() {
        var page = new PageQuery(0, 10, List.of(new PageQuery.Sort("Title", true)));

        assertThrows(InvalidSortPropertyException.class, () -> service.search(null, null, null, null, page));
    }

    // ---------------------------------------------------------------- findAllById

    @Test
    void findAllByIdReturnsWhatExistsSortedById() {
        when(movies.findAllById(List.of(2L, 99L, 1L))).thenReturn(List.of(stored(2L), stored(1L)));

        assertEquals(List.of(stored(1L), stored(2L)), service.findAllById(List.of(2L, 99L, 1L)));
    }

    @Test
    void findAllByIdWithoutIdsReturnsEmptyList() {
        when(movies.findAllById(List.of())).thenReturn(List.of());

        assertTrue(service.findAllById(List.of()).isEmpty());
    }
}
