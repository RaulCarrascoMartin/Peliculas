package shop.rating.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.mapper.MovieMarkMapper;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.repository.MovieMarkRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Rules of the favourite / watched lists with the repository and the catalog mocked; the mapper is the real one. */
class MovieMarkServiceTest {

    private static final CatalogMovieDto ALIEN = new CatalogMovieDto(10L, "Alien", 1979, "Horror", "Scott", 4.5);
    private static final CatalogMovieDto MATRIX = new CatalogMovieDto(11L, "Matrix", 1999, "Sci-fi", "W", null);

    private MovieMarkRepository marks;
    private MovieCatalog movies;
    private MovieMarkService service;

    @BeforeEach
    void setUp() {
        marks = mock(MovieMarkRepository.class);
        movies = mock(MovieCatalog.class);
        service = new MovieMarkServiceImpl(marks, movies, Mappers.getMapper(MovieMarkMapper.class));
        when(movies.exists(10L)).thenReturn(true);
        when(movies.exists(11L)).thenReturn(true);
    }

    private static MovieMarkDto request(Long userId, Long movieId, MarkType type) {
        return new MovieMarkDto(null, userId, movieId, type);
    }

    private void stored(Long id, Long userId, Long movieId, MarkType type) {
        when(marks.findByUserIdAndMovieIdAndType(userId, movieId, type))
                .thenReturn(Optional.of(new MovieMarkDto(id, userId, movieId, type)));
    }

    @Test
    void marksMovieForTheUserInThatList() {
        service.mark(request(1L, 10L, MarkType.FAVORITA));

        verify(marks).save(new MovieMarkDto(null, 1L, 10L, MarkType.FAVORITA));
    }

    @Test
    void markingSameMovieTwiceDoesNotDuplicateIt() {
        stored(5L, 1L, 10L, MarkType.VISTA);

        service.mark(request(1L, 10L, MarkType.VISTA));

        verify(marks, never()).save(any());
    }

    @Test
    void movieCanBeFavoriteAndWatched() {
        stored(5L, 1L, 10L, MarkType.FAVORITA);

        service.mark(request(1L, 10L, MarkType.VISTA));

        verify(marks).save(new MovieMarkDto(null, 1L, 10L, MarkType.VISTA));
    }

    @Test
    void markingUnknownMovieFailsWithoutSaving() {
        when(movies.exists(99L)).thenReturn(false);

        var ex = assertThrows(MovieNotFoundException.class,
                () -> service.mark(request(1L, 99L, MarkType.FAVORITA)));

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(marks);
    }

    @Test
    void unmarkDeletesOnlyTheMarkOfThatList() {
        stored(5L, 1L, 10L, MarkType.FAVORITA);
        stored(6L, 1L, 10L, MarkType.VISTA);

        service.unmark(request(1L, 10L, MarkType.FAVORITA));

        verify(marks).delete(5L);
        verify(marks, never()).delete(6L);
    }

    @Test
    void unmarkingUnmarkedMovieDoesNothing() {
        stored(5L, 1L, 10L, MarkType.FAVORITA);

        service.unmark(request(1L, 11L, MarkType.FAVORITA));
        service.unmark(request(1L, 10L, MarkType.VISTA));

        verify(marks, never()).delete(any());
    }

    @Test
    void eachUserChangesOnlyTheirOwnMarks() {
        stored(5L, 1L, 10L, MarkType.FAVORITA);

        service.unmark(request(2L, 10L, MarkType.FAVORITA));

        verify(marks).findByUserIdAndMovieIdAndType(2L, 10L, MarkType.FAVORITA);
        verify(marks, never()).delete(any());
    }

    @Test
    void unmarkingUnknownMovieDoesNotThrowNorAsksTheCatalog() {
        service.unmark(request(1L, 99L, MarkType.FAVORITA));

        verify(marks, never()).delete(any());
        verifyNoInteractions(movies);
    }

    @Test
    void listAsksTheCatalogForTheMarkedMoviesOfThatUserAndListAndReturnsTheirData() {
        when(marks.findByUserIdAndType(1L, MarkType.FAVORITA)).thenReturn(List.of(
                new MovieMarkDto(5L, 1L, 11L, MarkType.FAVORITA),
                new MovieMarkDto(6L, 1L, 10L, MarkType.FAVORITA)));
        when(movies.findByIds(List.of(11L, 10L))).thenReturn(List.of(ALIEN, MATRIX));

        List<CatalogMovieDto> marked = service.listMarked(1L, MarkType.FAVORITA);

        assertEquals(List.of(ALIEN, MATRIX), marked);
        assertEquals("Alien", marked.get(0).title());
        assertTrue(service.listMarked(1L, MarkType.VISTA).isEmpty());
        assertTrue(service.listMarked(2L, MarkType.FAVORITA).isEmpty());
    }

    @Test
    void markedMoviesThatNoLongerExistInTheCatalogAreLeftOut() {
        when(marks.findByUserIdAndType(1L, MarkType.VISTA)).thenReturn(List.of(
                new MovieMarkDto(5L, 1L, 10L, MarkType.VISTA),
                new MovieMarkDto(6L, 1L, 99L, MarkType.VISTA)));
        when(movies.findByIds(List.of(10L, 99L))).thenReturn(List.of(ALIEN));

        assertEquals(List.of(ALIEN), service.listMarked(1L, MarkType.VISTA));
    }

    @Test
    void withoutMarksListIsEmpty() {
        when(marks.findByUserIdAndType(1L, MarkType.VISTA)).thenReturn(List.of());
        when(movies.findByIds(List.of())).thenReturn(List.of());

        assertTrue(service.listMarked(1L, MarkType.VISTA).isEmpty());
    }
}
