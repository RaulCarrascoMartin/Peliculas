package shop.rating.internal.domain.service;

import shop.rating.internal.domain.contract.CatalogMovie;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.MarkType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.rating.internal.domain.fake.MovieCatalogFake;
import shop.rating.internal.domain.fake.MovieMarkRepositoryFake;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieMarkServiceTest {

    private MovieMarkRepositoryFake marks;
    private MovieMarkService service;

    @BeforeEach
    void setUp() {
        marks = new MovieMarkRepositoryFake();
        var movies = new MovieCatalogFake();
        movies.addMovie(10L);
        service = new MovieMarkServiceImpl(marks, movies);
    }

    @Test
    void markIsIdempotentAndListedPerType() {
        service.mark(1L, 10L, MarkType.FAVORITA);
        service.mark(1L, 10L, MarkType.FAVORITA);

        assertEquals(1, marks.count());
        assertEquals(List.of(10L), service.listMarked(1L, MarkType.FAVORITA).stream().map(CatalogMovie::id).toList());
        assertEquals(List.of(), service.listMarked(1L, MarkType.VISTA));
    }

    @Test
    void unmarkRemovesOnlyThatMarkAndUnknownMovieFailsToMark() {
        service.mark(1L, 10L, MarkType.FAVORITA);
        service.unmark(1L, 10L, MarkType.VISTA);
        assertEquals(1, marks.count());

        service.unmark(1L, 10L, MarkType.FAVORITA);
        assertEquals(0, marks.count());
        assertThrows(MovieNotFoundException.class, () -> service.mark(1L, 99L, MarkType.VISTA));
    }
}
