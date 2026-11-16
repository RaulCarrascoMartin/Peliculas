package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.model.MovieDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.catalog.internal.domain.fake.MovieRepositoryFake;
import shop.shared.pagination.PageQuery;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieServiceTest {

    private MovieService service;

    @BeforeEach
    void setUp() {
        service = new MovieServiceImpl(new MovieRepositoryFake());
    }

    private static MovieDetails details(String title, Integer stock) {
        return new MovieDetails(title, 1979, "Drama", "Ridley Scott", new BigDecimal("9.99"), stock);
    }

    @Test
    void createDefaultsStockToZeroAndUpdateKeepsStockWhenNull() {
        var movie = service.create(details("Alien", null));
        assertEquals(0, movie.getStock());

        service.update(movie.getId(), details("Aliens", 7));
        service.update(movie.getId(), details("Alien 3", null));

        assertEquals(7, service.find(movie.getId()).getStock());
        assertEquals("Alien 3", service.find(movie.getId()).getTitle());
    }

    @Test
    void unknownMovieFailsOnFindUpdateDeleteAndAverage() {
        assertThrows(MovieNotFoundException.class, () -> service.find(9L));
        assertThrows(MovieNotFoundException.class, () -> service.update(9L, details("X", 1)));
        assertThrows(MovieNotFoundException.class, () -> service.delete(9L));
        assertThrows(MovieNotFoundException.class, () -> service.updateAverageRating(9L, 3.0));
    }

    @Test
    void searchIgnoresBlankFiltersAndFindAllByIdIsSortedAndIgnoresUnknown() {
        var a = service.create(details("Alien", 1));
        var b = service.create(details("Blade", 1));

        assertEquals(2, service.search(" ", "", null, "  ", PageQuery.of(0, 10)).total());
        assertEquals(List.of(a.getId(), b.getId()),
                service.findAllById(List.of(b.getId(), 99L, a.getId())).stream().map(Movie::getId).toList());
    }
}
