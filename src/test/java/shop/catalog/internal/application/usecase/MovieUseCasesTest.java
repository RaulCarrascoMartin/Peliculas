package shop.catalog.internal.application.usecase;

import shop.catalog.internal.application.dto.MovieData;
import shop.catalog.internal.application.facade.CatalogFacadeImpl;
import shop.catalog.internal.domain.fake.MovieRepositoryFake;

import shop.catalog.internal.domain.service.MovieServiceImpl;
import shop.catalog.internal.domain.service.StockServiceImpl;
import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.facade.dto.MovieSummaryDto;
import shop.catalog.internal.application.facade.CatalogFacadeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.shared.pagination.PageQuery;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieUseCasesTest {

    private MovieRepositoryFake repository;
    private CreateMovie create;
    private GetMovie get;
    private SearchMovies search;
    private UpdateMovie update;
    private DeleteMovie delete;
    private CatalogFacadeImpl api;

    @BeforeEach
    void setUp() {
        repository = new MovieRepositoryFake();
        var service = new MovieServiceImpl(repository);
        var stock = new StockServiceImpl(repository);
        create = new CreateMovie(service);
        get = new GetMovie(service);
        search = new SearchMovies(service);
        update = new UpdateMovie(service);
        delete = new DeleteMovie(service);
        api = new CatalogFacadeImpl(service, new ReserveStock(stock), new ReleaseStock(stock));
    }

    private static MovieData movie(String title, int year, String genre, String director) {
        return new MovieData(title, year, genre, director, null, null);
    }

    private static MovieData withPrice(MovieData data, String price) {
        return new MovieData(data.title(), data.year(), data.genre(), data.director(), new BigDecimal(price), data.stock());
    }

    @Test
    void createsMovieAndReturnsAssignedId() {
        var created = create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott"));

        assertNotNull(created.id());
        assertEquals("Alien", repository.findById(created.id()).orElseThrow().getTitle());
    }

    @Test
    void returnsDetailOfExistingMovie() {
        var id = create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott")).id();

        assertEquals("Alien", get.execute(id).title());
    }

    @Test
    void detailOfUnknownMovieThrows404() {
        assertThrows(ResourceNotFoundException.class, () -> get.execute(99L));
    }

    @Test
    void updatesDataAndKeepsAverageRating() {
        var original = movie("Alien", 1979, "Science fiction", "Ridley Scott");
        var id = create.execute(original).id();
        api.updateAverageRating(id, 4.5);

        var updated = update.execute(id, movie("Aliens", 1986, "Action", "James Cameron"));

        assertEquals(id, updated.id());
        assertEquals("Aliens", updated.title());
        assertEquals(1986, updated.year());
        assertEquals("Action", updated.genre());
        assertEquals("James Cameron", updated.director());
        assertEquals(4.5, updated.averageRating());
        assertEquals(1, repository.count());
    }

    @Test
    void updatingUnknownMovieThrows404() {
        assertThrows(ResourceNotFoundException.class,
                () -> update.execute(99L, movie("Aliens", 1986, "Action", "James Cameron")));
        assertEquals(0, repository.count());
    }

    @Test
    void deletesMovie() {
        var id = create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott")).id();

        delete.execute(id);

        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void deletingUnknownMovieThrows404() {
        assertThrows(ResourceNotFoundException.class, () -> delete.execute(99L));
    }

    @Test
    void existsReturnsTrueOnlyForCreatedMovies() {
        var id = create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott")).id();

        assertTrue(api.exists(id));
        assertFalse(api.exists(99L));
    }

    @Test
    void updatesAverageRatingWithoutTouchingTheRest() {
        var id = create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott")).id();

        api.updateAverageRating(id, 3.5);

        var saved = repository.findById(id).orElseThrow();
        assertEquals(3.5, saved.getAverageRating());
        assertEquals("Alien", saved.getTitle());
    }

    @Test
    void updatingAverageRatingOfUnknownMovieThrows404() {
        assertThrows(ResourceNotFoundException.class, () -> api.updateAverageRating(99L, 3.5));
    }

    @Test
    void listsMoviesPaged() {
        for (int i = 1; i <= 5; i++) {
            create.execute(movie("Película " + i, 2000 + i, "Drama", "Director"));
        }

        var page = search.execute(null, null, null, null, PageQuery.of(1, 2));

        assertEquals(5, page.total());
        assertEquals(3, (int) Math.ceil(page.total() / 2.0));
        assertEquals(2, page.content().size());
        assertEquals("Película 3", page.content().get(0).title());
    }

    @Test
    void listWithoutMoviesReturnsEmptyPage() {
        assertTrue(search.execute(null, null, null, null, PageQuery.of(0, 10)).isEmpty());
    }

    private void createCatalog() {
        create.execute(movie("Alien", 1979, "Science fiction", "Ridley Scott"));
        create.execute(movie("Aliens", 1986, "Science fiction", "James Cameron"));
        create.execute(movie("Gladiator", 2000, "Drama", "Ridley Scott"));
        create.execute(movie("Titanic", 1997, "Drama", "James Cameron"));
    }

    private java.util.List<String> titles(String title, String genre, Integer year, String director) {
        return search.execute(title, genre, year, director, PageQuery.of(0, 10)).content().stream()
                .map(MovieSummaryDto::title).toList();
    }

    @Test
    void searchesByTitleFragmentIgnoringCase() {
        createCatalog();

        assertEquals(java.util.List.of("Alien", "Aliens"), titles("ALIEN", null, null, null));
        assertEquals(java.util.List.of("Gladiator"), titles("diat", null, null, null));
    }

    @Test
    void filtersByGenreYearAndDirector() {
        createCatalog();

        assertEquals(java.util.List.of("Gladiator", "Titanic"), titles(null, "Drama", null, null));
        assertEquals(java.util.List.of("Titanic"), titles(null, null, 1997, null));
        assertEquals(java.util.List.of("Alien", "Gladiator"), titles(null, null, null, "Ridley Scott"));
    }

    @Test
    void combinesAllGivenFilters() {
        createCatalog();

        assertEquals(java.util.List.of("Gladiator"), titles(null, "Drama", null, "Ridley Scott"));
        assertEquals(java.util.List.of("Aliens"), titles("alien", "Science fiction", 1986, "James Cameron"));
        assertTrue(titles("alien", "Drama", null, null).isEmpty());
    }

    @Test
    void ignoresBlankFilters() {
        createCatalog();

        assertEquals(4, titles("  ", "", null, " ").size());
    }

    @Test
    void noMatchesReturnsEmptyList() {
        createCatalog();

        assertTrue(titles("zzz", null, null, null).isEmpty());
        assertTrue(titles(null, null, 1800, null).isEmpty());
    }

    @Test
    void summariesReturnRequestedMoviesSortedByIdIgnoringUnknown() {
        var alien = withPrice(movie("Alien", 1979, "Science fiction", "Ridley Scott"), "12.99");
        var gladiator = movie("Gladiator", 2000, "Drama", "Ridley Scott");
        // preexisting without price
        var alienId = create.execute(alien).id();
        var gladiatorId = create.execute(gladiator).id();
        api.updateAverageRating(gladiatorId, 4.5);

        var findSummaries = api.findSummaries(java.util.List.of(gladiatorId, alienId, 99L));

        assertEquals(2, findSummaries.size());
        assertEquals("Alien", findSummaries.get(0).title());
        assertEquals(new BigDecimal("12.99"), findSummaries.get(0).price());
        assertEquals("Gladiator", findSummaries.get(1).title());
        assertEquals(2000, findSummaries.get(1).year());
        assertEquals("Drama", findSummaries.get(1).genre());
        assertEquals("Ridley Scott", findSummaries.get(1).director());
        assertNull(findSummaries.get(1).price());
        assertEquals(4.5, findSummaries.get(1).averageRating());
    }

    @Test
    void storePriceOnCreate() {
        var alien = withPrice(movie("Alien", 1979, "Science fiction", "Ridley Scott"), "12.99");

        var id = create.execute(alien).id();

        assertEquals(new BigDecimal("12.99"), get.execute(id).price());
    }

    @Test
    void updateChangesPriceAndKeepsTheRest() {
        var alien = withPrice(movie("Alien", 1979, "Science fiction", "Ridley Scott"), "12.99");
        var id = create.execute(alien).id();

        var aliens = withPrice(movie("Alien (remastered)", 1979, "Science fiction", "Ridley Scott"), "19.99");
        var updated = update.execute(id, aliens);

        assertEquals(new BigDecimal("19.99"), updated.price());
        assertEquals(new BigDecimal("19.99"), repository.findById(id).orElseThrow().getPrice());
    }

    @Test
    void summariesWithoutIdsReturnEmptyList() {
        assertTrue(api.findSummaries(java.util.List.of()).isEmpty());
    }
}
