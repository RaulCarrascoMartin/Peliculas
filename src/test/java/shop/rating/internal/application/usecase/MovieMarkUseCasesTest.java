package shop.rating.internal.application.usecase;

import shop.rating.internal.application.dto.MarkedMovieView;
import shop.rating.internal.domain.fake.MovieCatalogFake;
import shop.rating.internal.domain.fake.MovieMarkRepositoryFake;

import shop.shared.error.ResourceNotFoundException;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieMarkUseCasesTest {

    private MovieMarkRepositoryFake repository;
    private MovieCatalogFake movies;

    @BeforeEach
    void setUp() {
        repository = new MovieMarkRepositoryFake();
        movies = new MovieCatalogFake();
        movies.addMovie(10L);
        movies.addMovie(11L);
    }

    /** The three mark use cases for the same current user. */
    private record Marks(Long userId, MarkMovie markMovie, UnmarkMovie unmarkMovie,
                          ListMarkedMovies listMarked) {
        void mark(Long movieId, MarkType type) {
            markMovie.execute(userId, movieId, type);
        }

        void unmark(Long movieId, MarkType type) {
            unmarkMovie.execute(userId, movieId, type);
        }

        List<MarkedMovieView> markedMovies(MarkType type) {
            return listMarked.execute(userId, type);
        }
    }

    private Marks serviceFor(Long userId) {
        var service = new MovieMarkServiceImpl(repository, movies);
        return new Marks(userId, new MarkMovie(service), new UnmarkMovie(service), new ListMarkedMovies(service));
    }

    private List<Long> markedIds(Long userId, MarkType type) {
        return serviceFor(userId).markedMovies(type).stream().map(MarkedMovieView::id).toList();
    }

    @Test
    void marksMovieAsFavoriteForCurrentUser() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);

        assertEquals(List.of(10L), markedIds(1L, MarkType.FAVORITA));
        assertTrue(serviceFor(1L).markedMovies(MarkType.VISTA).isEmpty());
    }

    @Test
    void markingSameMovieTwiceDoesNotDuplicateIt() {
        serviceFor(1L).mark(10L, MarkType.VISTA);
        serviceFor(1L).mark(10L, MarkType.VISTA);

        assertEquals(1, repository.count());
    }

    @Test
    void movieCanBeFavoriteAndWatched() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);
        serviceFor(1L).mark(10L, MarkType.VISTA);

        assertEquals(List.of(10L), markedIds(1L, MarkType.FAVORITA));
        assertEquals(List.of(10L), markedIds(1L, MarkType.VISTA));
    }

    @Test
    void markingUnknownMovieThrows404WithoutSaving() {
        var service = serviceFor(1L);

        assertThrows(ResourceNotFoundException.class, () -> service.mark(99L, MarkType.FAVORITA));
        assertEquals(0, repository.count());
    }

    @Test
    void unmarksMovieOnlyFromThatList() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);
        serviceFor(1L).mark(10L, MarkType.VISTA);

        serviceFor(1L).unmark(10L, MarkType.FAVORITA);

        assertTrue(serviceFor(1L).markedMovies(MarkType.FAVORITA).isEmpty());
        assertEquals(List.of(10L), markedIds(1L, MarkType.VISTA));
    }

    @Test
    void unmarkingUnmarkedMovieDoesNothing() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);

        serviceFor(1L).unmark(11L, MarkType.FAVORITA);
        serviceFor(1L).unmark(10L, MarkType.VISTA);

        assertEquals(1, repository.count());
    }

    @Test
    void eachUserSeesAndChangesOnlyTheirOwnMarks() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);
        serviceFor(2L).mark(11L, MarkType.FAVORITA);

        assertEquals(List.of(10L), markedIds(1L, MarkType.FAVORITA));
        assertEquals(List.of(11L), markedIds(2L, MarkType.FAVORITA));

        serviceFor(2L).unmark(10L, MarkType.FAVORITA);

        assertEquals(List.of(10L), markedIds(1L, MarkType.FAVORITA));
    }

    @Test
    void listIncludesMovieData() {
        serviceFor(1L).mark(10L, MarkType.FAVORITA);

        var marked = serviceFor(1L).markedMovies(MarkType.FAVORITA);

        assertEquals(1, marked.size());
        assertEquals("Película 10", marked.get(0).title());
    }

    @Test
    void unmarkingUnknownMovieDoesNotThrow() {
        serviceFor(1L).unmark(99L, MarkType.FAVORITA);

        assertEquals(0, repository.count());
    }

    @Test
    void withoutMarksListIsEmpty() {
        assertTrue(serviceFor(1L).markedMovies(MarkType.VISTA).isEmpty());
    }
}
