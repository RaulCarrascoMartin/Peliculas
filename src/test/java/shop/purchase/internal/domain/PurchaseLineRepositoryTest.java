package shop.purchase.internal.domain;

import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class PurchaseLineRepositoryTest {

    @Autowired
    private PurchaseLineRepository lines;

    @Autowired
    private MovieRepository movies;

    private PurchaseLine line(Long purchaseId, Long movieId, String title, String price) {
        var line = new PurchaseLine();
        line.setPurchaseId(purchaseId);
        line.setMovieId(movieId);
        line.setTitle(title);
        line.setUnitPrice(new BigDecimal(price));
        return line;
    }

    @Test
    void savesLineWithTitleAndPriceSnapshot() {
        var saved = lines.saveAndFlush(line(1L, 10L, "Matrix", "7.99"));

        var read = lines.findById(saved.getId()).orElseThrow();
        assertEquals("Matrix", read.getTitle());
        assertEquals(new BigDecimal("7.99"), read.getUnitPrice());
        assertEquals(10L, read.getMovieId());
    }

    @Test
    void listsPurchaseLinesInInsertionOrder() {
        lines.saveAndFlush(line(1L, 12L, "Inception", "9.49"));
        lines.saveAndFlush(line(1L, 10L, "Matrix", "7.99"));
        lines.saveAndFlush(line(2L, 99L, "Other", "1.00"));

        List<String> titles = lines.findByPurchaseIdOrderById(1L).stream().map(PurchaseLine::getTitle).toList();

        assertEquals(List.of("Inception", "Matrix"), titles);
    }

    @Test
    void historyKeepsTitleAndPriceEvenIfMovieIsUpdatedOrDeleted() {
        var movie = new Movie();
        movie.setTitle("Matrix");
        movie.setPrice(new BigDecimal("7.99"));
        var saved = movies.saveAndFlush(movie);
        lines.saveAndFlush(line(1L, saved.getId(), saved.getTitle(), "7.99"));

        saved.setTitle("Matrix Reloaded");
        saved.setPrice(new BigDecimal("14.99"));
        movies.saveAndFlush(saved);

        var afterUpdate = lines.findByPurchaseIdOrderById(1L).get(0);
        assertEquals("Matrix", afterUpdate.getTitle());
        assertEquals(new BigDecimal("7.99"), afterUpdate.getUnitPrice());

        movies.deleteById(saved.getId());
        movies.flush();

        var afterDelete = lines.findByPurchaseIdOrderById(1L);
        assertEquals(1, afterDelete.size());
        assertEquals("Matrix", afterDelete.get(0).getTitle());
        assertEquals(new BigDecimal("7.99"), afterDelete.get(0).getUnitPrice());
        assertTrue(movies.findById(saved.getId()).isEmpty());
    }
}
