package shop.purchase.internal.persistence.repository;

import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcPurchaseLineRepository.class)
class PurchaseLineRepositoryTest {

    @Autowired
    private PurchaseLineRepository lines;

    @Autowired
    private JdbcClient jdbc;



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
        var saved = lines.save(line(1L, 10L, "Matrix", "7.99"));

        var read = lines.findByPurchaseIdOrderById(1L).get(0);
        assertEquals("Matrix", read.getTitle());
        assertEquals(new BigDecimal("7.99"), read.getUnitPrice());
        assertEquals(10L, read.getMovieId());
    }

    @Test
    void listsPurchaseLinesInInsertionOrder() {
        lines.save(line(1L, 12L, "Inception", "9.49"));
        lines.save(line(1L, 10L, "Matrix", "7.99"));
        lines.save(line(2L, 99L, "Other", "1.00"));

        List<String> titles = lines.findByPurchaseIdOrderById(1L).stream().map(PurchaseLine::getTitle).toList();

        assertEquals(List.of("Inception", "Matrix"), titles);
    }

    @Test
    void historyKeepsTitleAndPriceEvenIfMovieIsUpdatedOrDeleted() {
        jdbc.sql("insert into peliculas (id, titulo, precio) values (500, 'Matrix', 7.99)").update();
        lines.save(line(1L, 500L, "Matrix", "7.99"));

        jdbc.sql("update peliculas set titulo = 'Matrix Reloaded', precio = 14.99 where id = 500").update();

        var afterUpdate = lines.findByPurchaseIdOrderById(1L).get(0);
        assertEquals("Matrix", afterUpdate.getTitle());
        assertEquals(new BigDecimal("7.99"), afterUpdate.getUnitPrice());

        jdbc.sql("delete from peliculas where id = 500").update();

        var afterDelete = lines.findByPurchaseIdOrderById(1L);
        assertEquals(1, afterDelete.size());
        assertEquals("Matrix", afterDelete.get(0).getTitle());
        assertEquals(new BigDecimal("7.99"), afterDelete.get(0).getUnitPrice());
        assertTrue(jdbc.sql("select count(*) from peliculas where id = 500").query(Long.class).single() == 0);
    }
}
