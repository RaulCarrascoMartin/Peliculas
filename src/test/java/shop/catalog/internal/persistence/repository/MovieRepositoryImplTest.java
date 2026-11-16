package shop.catalog.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.shared.MariaDbRepositoryTest;
import shop.shared.pagination.PageQuery;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of movies against a real MariaDB. */
@Import(MovieRepositoryImpl.class)
class MovieRepositoryImplTest extends MariaDbRepositoryTest {

    private static final PageQuery FIRST_TEN = PageQuery.of(0, 10);

    @Autowired
    private MovieRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static MovieDto movie(String title, Integer year, String genre, String director) {
        return new MovieDto(null, title, year, genre, director, new BigDecimal("9.99"), null, 0);
    }

    private static MovieDto alien() {
        return new MovieDto(null, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 4.5, 3);
    }

    private Long withStock(int stock) {
        return repository.save(new MovieDto(null, "Alien", 1979, "Drama", "X", new BigDecimal("9.99"), null, stock))
                .id();
    }

    private int count() {
        return jdbc.sql("select count(*) from peliculas").query(Integer.class).single();
    }

    private void catalog() {
        repository.save(movie("Alien", 1979, "Science fiction", "Ridley Scott"));
        repository.save(movie("Aliens", 1986, "Science fiction", "James Cameron"));
        repository.save(movie("Gladiator", 2000, "Drama", "Ridley Scott"));
        repository.save(movie("Titanic", 1997, "Drama", "James Cameron"));
    }

    private List<String> titles(String title, String genre, Integer year, String director) {
        return repository.search(title, genre, year, director, FIRST_TEN).content().stream()
                .map(MovieDto::title).toList();
    }

    private static PageQuery sortedBy(String property, boolean ascending) {
        return new PageQuery(0, 10, List.of(new PageQuery.Sort(property, ascending)));
    }

    // ---------------------------------------------------------------- save and find

    @Test
    void saveAssignsIdAndFindByIdReturnsEveryColumn() {
        MovieDto saved = repository.save(alien());

        assertNotNull(saved.id());
        MovieDto found = repository.findById(saved.id()).orElseThrow();
        assertEquals(saved, found);
        assertEquals(new MovieDto(saved.id(), "Alien", 1979, "Science fiction", "Ridley Scott",
                new BigDecimal("12.99"), 4.5, 3), found);
    }

    @Test
    void optionalColumnsAreStoredAndReadAsNull() {
        Long id = repository.save(new MovieDto(null, "Silent", null, null, null, null, null, 0)).id();

        assertEquals(new MovieDto(id, "Silent", null, null, null, null, null, 0),
                repository.findById(id).orElseThrow());
    }

    @Test
    void saveWithIdOfAMissingMovieThrowsNotFound() {
        assertThrows(MovieNotFoundException.class,
                () -> repository.save(new MovieDto(999_999L, "Ghost", null, null, null, null, null, 0)));
    }

    @Test
    void updateAverageRatingChangesOnlyThatColumnAndReportsMissingMovies() {
        Long id = repository.save(alien()).id();

        assertEquals(1, repository.updateAverageRating(id, 2.5));
        assertEquals(new MovieDto(id, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 2.5, 3),
                repository.findById(id).orElseThrow());
        assertEquals(0, repository.updateAverageRating(999_999L, 1.0));
    }

    @Test
    void saveWithIdUpdatesDetailsAndStockButNeverTheAverageRating() {
        Long id = repository.save(alien()).id();
        Long other = repository.save(movie("Gladiator", 2000, "Drama", "Ridley Scott")).id();
        var changed = new MovieDto(id, "Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"), null, 7);

        assertEquals(changed, repository.save(changed));

        assertEquals(new MovieDto(id, "Aliens", 1986, "Action", "James Cameron", new BigDecimal("19.99"), 4.5, 7),
                repository.findById(id).orElseThrow());
        assertEquals("Gladiator", repository.findById(other).orElseThrow().title());
        assertEquals(2, count());
    }

    @Test
    void movieWithoutTitleIsRejectedAndLeavesNoRow() {
        int before = count();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(movie(null, 1979, "Drama", "X")));

        assertEquals(before, count());
    }

    @Test
    void findByIdOfUnknownMovieIsEmpty() {
        assertTrue(repository.findById(987654L).isEmpty());
    }

    @Test
    void existsByIdOnlyForStoredMovies() {
        Long id = repository.save(alien()).id();

        assertTrue(repository.existsById(id));
        assertFalse(repository.existsById(987654L));
    }

    @Test
    void deleteByIdRemovesOnlyThatMovieAndIgnoresUnknownIds() {
        Long id = repository.save(alien()).id();
        Long other = repository.save(alien()).id();

        repository.deleteById(id);
        repository.deleteById(987654L);

        assertTrue(repository.findById(id).isEmpty());
        assertTrue(repository.existsById(other));
    }

    @Test
    void findAllByIdReturnsTheExistingOnesAndIgnoresUnknownIds() {
        Long alien = repository.save(alien()).id();
        Long gladiator = repository.save(movie("Gladiator", 2000, "Drama", "Ridley Scott")).id();
        repository.save(movie("Titanic", 1997, "Drama", "James Cameron"));

        List<MovieDto> found = repository.findAllById(List.of(gladiator, 987654L, alien));

        assertEquals(2, found.size());
        assertTrue(found.contains(repository.findById(alien).orElseThrow()));
        assertTrue(found.contains(repository.findById(gladiator).orElseThrow()));
    }

    @Test
    void findAllByIdWithoutIdsOrWithoutMatchesIsEmpty() {
        repository.save(alien());

        assertTrue(repository.findAllById(List.of()).isEmpty());
        assertTrue(repository.findAllById(List.of(987654L)).isEmpty());
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchMatchesTheTitleByFragmentIgnoringCase() {
        catalog();

        assertEquals(List.of("Alien", "Aliens"), titles("ALIEN", null, null, null));
        assertEquals(List.of("Gladiator"), titles("diat", null, null, null));
    }

    @Test
    void searchFiltersByGenreYearAndDirector() {
        catalog();

        assertEquals(List.of("Gladiator", "Titanic"), titles(null, "Drama", null, null));
        assertEquals(List.of("Titanic"), titles(null, null, 1997, null));
        assertEquals(List.of("Alien", "Gladiator"), titles(null, null, null, "Ridley Scott"));
    }

    @Test
    void searchCombinesAllGivenFilters() {
        catalog();

        assertEquals(List.of("Gladiator"), titles(null, "Drama", null, "Ridley Scott"));
        assertEquals(List.of("Aliens"), titles("alien", "Science fiction", 1986, "James Cameron"));
        assertTrue(titles("alien", "Drama", null, null).isEmpty());
    }

    @Test
    void searchWithoutFiltersReturnsEverythingSortedById() {
        catalog();

        assertEquals(List.of("Alien", "Aliens", "Gladiator", "Titanic"), titles(null, null, null, null));
    }

    @Test
    void searchWithoutMatchesIsAnEmptyPageWithZeroTotal() {
        catalog();

        var page = repository.search("zzz", null, null, null, FIRST_TEN);

        assertTrue(page.isEmpty());
        assertEquals(0, page.total());
        assertTrue(repository.search(null, null, 1800, null, FIRST_TEN).isEmpty());
    }

    @Test
    void searchReturnsTheRequestedPageAndTheTotalOfTheFilter() {
        for (int i = 1; i <= 5; i++) {
            repository.save(movie("Película " + i, 2000 + i, "Drama", "Director"));
        }
        repository.save(movie("Other", 1990, "Western", "Director"));

        var page = repository.search(null, "Drama", null, null, PageQuery.of(1, 2));

        assertEquals(5, page.total());
        assertEquals(3, page.totalPages());
        assertEquals(List.of("Película 3", "Película 4"), page.content().stream().map(MovieDto::title).toList());
        assertEquals(PageQuery.of(1, 2), page.query());
        assertEquals(1, repository.search(null, "Drama", null, null, PageQuery.of(2, 2)).content().size());
        assertTrue(repository.search(null, "Drama", null, null, PageQuery.of(3, 2)).isEmpty());
    }

    @Test
    void searchSortsByTheRequestedPropertiesInBothDirections() {
        catalog();

        assertEquals(List.of("Titanic", "Gladiator", "Aliens", "Alien"),
                repository.search(null, null, null, null, sortedBy("title", false)).content().stream()
                        .map(MovieDto::title).toList());
        assertEquals(List.of("Alien", "Aliens", "Titanic", "Gladiator"),
                repository.search(null, null, null, null, sortedBy("year", true)).content().stream()
                        .map(MovieDto::title).toList());
    }

    @Test
    void searchBreaksTiesByIdAndCombinesSeveralSorts() {
        catalog();
        var byGenreThenYearDesc = new PageQuery(0, 10,
                List.of(new PageQuery.Sort("genre", true), new PageQuery.Sort("year", false)));

        assertEquals(List.of("Gladiator", "Titanic", "Alien", "Aliens"),
                repository.search(null, null, null, null, sortedBy("genre", true)).content().stream()
                        .map(MovieDto::title).toList());
        assertEquals(List.of("Gladiator", "Titanic", "Aliens", "Alien"),
                repository.search(null, null, null, null, byGenreThenYearDesc).content().stream()
                        .map(MovieDto::title).toList());
    }

    @Test
    void everySortablePropertyHasAColumn() {
        catalog();

        for (String property : List.of("id", "title", "year", "genre", "director", "averageRating", "price", "stock")) {
            assertEquals(4, repository.search(null, null, null, null, sortedBy(property, true)).content().size());
        }
    }

    @Test
    void sortPropertyWithoutColumnNeverReachesTheSql() {
        assertThrows(IllegalArgumentException.class, () -> repository.search(null, null, null, null,
                sortedBy("titulo; drop table peliculas", true)));

        assertEquals(0, count());
    }

    // ---------------------------------------------------------------- stock

    @Test
    void decrementStockSubtractsOnlyWhenEnoughUnits() {
        Long id = withStock(5);

        assertEquals(1, repository.decrementStock(id, 3));
        assertEquals(0, repository.decrementStock(id, 3));
        assertEquals(2, repository.findById(id).orElseThrow().stock());
        assertEquals(1, repository.decrementStock(id, 2));
        assertEquals(0, repository.findById(id).orElseThrow().stock());
    }

    @Test
    void decrementStockOfUnknownMovieAffectsNothing() {
        assertEquals(0, repository.decrementStock(987654L, 1));
    }

    @Test
    void incrementStockAddsUnitsAndIgnoresUnknownMovies() {
        Long id = withStock(1);

        assertEquals(1, repository.incrementStock(id, 4));
        assertEquals(0, repository.incrementStock(987654L, 4));
        assertEquals(5, repository.findById(id).orElseThrow().stock());
    }

    @Test
    void stockChangesTouchOnlyThatMovie() {
        Long id = withStock(5);
        Long other = withStock(5);

        repository.decrementStock(id, 2);
        repository.incrementStock(id, 1);

        assertEquals(4, repository.findById(id).orElseThrow().stock());
        assertEquals(5, repository.findById(other).orElseThrow().stock());
    }
}
