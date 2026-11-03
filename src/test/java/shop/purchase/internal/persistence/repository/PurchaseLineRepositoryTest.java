package shop.purchase.internal.persistence.repository;

import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import shop.catalog.internal.persistence.jpa.MovieJpaRepository;
import shop.catalog.internal.persistence.entity.MovieEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(PurchaseLineRepositoryImpl.class)
class PurchaseLineRepositoryTest {

    @Autowired
    private PurchaseLineRepository lines;

    @Autowired
    private MovieJpaRepository movies;

    @Autowired
    private EntityManager em;

    private <T> T flushed(T saved) {
        em.flush();
        return saved;
    }

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
        var saved = flushed(lines.save(line(1L, 10L, "Matrix", "7.99")));

        var read = lines.findByPurchaseIdOrderById(1L).get(0);
        assertEquals("Matrix", read.getTitle());
        assertEquals(new BigDecimal("7.99"), read.getUnitPrice());
        assertEquals(10L, read.getMovieId());
    }

    @Test
    void listsPurchaseLinesInInsertionOrder() {
        flushed(lines.save(line(1L, 12L, "Inception", "9.49")));
        flushed(lines.save(line(1L, 10L, "Matrix", "7.99")));
        flushed(lines.save(line(2L, 99L, "Other", "1.00")));

        List<String> titles = lines.findByPurchaseIdOrderById(1L).stream().map(PurchaseLine::getTitle).toList();

        assertEquals(List.of("Inception", "Matrix"), titles);
    }

    @Test
    void historyKeepsTitleAndPriceEvenIfMovieIsUpdatedOrDeleted() {
        var movie = new MovieEntity();
        movie.setTitle("Matrix");
        movie.setPrice(new BigDecimal("7.99"));
        var saved = movies.saveAndFlush(movie);
        flushed(lines.save(line(1L, saved.getId(), saved.getTitle(), "7.99")));

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
