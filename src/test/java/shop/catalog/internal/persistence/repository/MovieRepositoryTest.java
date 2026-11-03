package shop.catalog.internal.persistence.repository;

import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(MovieRepositoryImpl.class)
class MovieRepositoryTest {

    @Autowired
    private MovieRepository repository;

    @Autowired
    private EntityManager em;

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
    }

    @Autowired
    private EntityManager entityManager;

    private Movie movie(String title) {
        var movie = new Movie();
        movie.setTitle(title);
        movie.setYear(1979);
        movie.setGenre("Science fiction");
        movie.setDirector("Ridley Scott");
        return movie;
    }

    @Test
    void savesMovieAndAssignsId() {
        var saved = flushed(repository.save(movie("Alien")));

        assertNotNull(saved.getId());
    }

    @Test
    void retrievesAllData() {
        var id = flushed(repository.save(movie("Alien"))).getId();
        entityManager.clear();

        var retrieved = repository.findById(id).orElseThrow();

        assertEquals("Alien", retrieved.getTitle());
        assertEquals(1979, retrieved.getYear());
        assertEquals("Science fiction", retrieved.getGenre());
        assertEquals("Ridley Scott", retrieved.getDirector());
    }

    @Test
    void withoutRatingsAverageIsNull() {
        var saved = flushed(repository.save(movie("Alien")));

        assertNull(saved.getAverageRating());
    }

    @Test
    void savesAverageRating() {
        var movie = movie("Alien");
        movie.setAverageRating(4.5);
        var id = flushed(repository.save(movie)).getId();
        entityManager.clear();

        assertEquals(4.5, repository.findById(id).orElseThrow().getAverageRating());
    }

    @Test
    void titleIsRequired() {
        assertThrows(DataIntegrityViolationException.class, () -> flushed(repository.save(movie(null))));
    }

    @Test
    void deletesMovie() {
        var id = flushed(repository.save(movie("Alien"))).getId();

        repository.deleteById(id);

        assertTrue(repository.findById(id).isEmpty());
    }

    private void save(String title, int year, String genre, String director) {
        var movie = movie(title);
        movie.setYear(year);
        movie.setGenre(genre);
        movie.setDirector(director);
        flushed(repository.save(movie));
    }

    @Test
    void searchIgnoresCaseAndCombinesFilters() {
        save("Alien", 1979, "Science fiction", "Ridley Scott");
        save("Aliens", 1986, "Science fiction", "James Cameron");
        save("Gladiator", 2000, "Drama", "Ridley Scott");
        var page = shop.shared.pagination.PageQuery.of(0, 10);

        assertEquals(2, repository.search("ALIEN", null, null, null, page).total());
        assertEquals(1, repository.search("alien", null, 1986, null, page).total());
        assertEquals(1, repository.search(null, "Drama", null, "Ridley Scott", page).total());
        assertEquals(3, repository.search(null, null, null, null, page).total());
        assertTrue(repository.search("alien", "Drama", null, null, page).isEmpty());
    }

    private Long withStock(String title, int stock) {
        var p = movie(title);
        p.setStock(stock);
        return flushed(repository.save(p)).getId();
    }

    @Test
    void decrementStockSubtractsOnlyWhenEnoughUnits() {
        Long id = withStock("Alien", 5);

        assertEquals(1, repository.decrementStock(id, 3));
        assertEquals(0, repository.decrementStock(id, 3));
        assertEquals(2, repository.findById(id).orElseThrow().getStock());
    }

    @Test
    void incrementStockAddsUnitsAndIgnoresUnknownProducts() {
        Long id = withStock("Alien", 1);

        assertEquals(1, repository.incrementStock(id, 4));
        assertEquals(0, repository.incrementStock(999L, 4));
        assertEquals(5, repository.findById(id).orElseThrow().getStock());
    }

    @Test
    void newMovieStartsWithZeroStock() {
        Long id = flushed(repository.save(movie("No stock"))).getId();

        assertEquals(0, repository.findById(id).orElseThrow().getStock());
    }
}
