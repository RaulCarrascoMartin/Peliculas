package shop.purchase.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseRepository;
import shop.shared.MariaDbRepositoryTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of purchases and their lines against a real MariaDB. */
@Import(PurchaseRepositoryImpl.class)
class PurchaseRepositoryImplTest extends MariaDbRepositoryTest {

    private static final LocalDateTime PURCHASED_AT = LocalDateTime.of(2026, 11, 2, 9, 0);

    @Autowired
    private PurchaseRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static PurchaseLineDto line(Long movieId, String title, String price) {
        return new PurchaseLineDto(null, null, movieId, title, new BigDecimal(price));
    }

    private static PurchaseDto purchase(Long userId, LocalDateTime purchasedAt, PurchaseLineDto... lines) {
        return new PurchaseDto(null, userId, purchasedAt, new BigDecimal("17.48"), PurchaseStatus.COMPRADA,
                List.of(lines));
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }

    @Test
    void saveAssignsIdsAndFindReturnsEveryColumnAndTheLinesInInsertionOrder() {
        PurchaseDto saved = repository.save(
                purchase(1L, PURCHASED_AT, line(12L, "Inception", "9.49"), line(10L, "Matrix", "7.99")));

        assertNotNull(saved.id());
        assertEquals(2, saved.lines().size());
        assertNotNull(saved.lines().get(0).id());
        assertEquals(saved.id(), saved.lines().get(0).purchaseId());

        PurchaseDto found = repository.findByIdAndUserId(saved.id(), 1L).orElseThrow();
        assertEquals(saved, found);
        assertEquals(1L, found.userId());
        assertEquals(PURCHASED_AT, found.purchasedAt());
        assertEquals(new BigDecimal("17.48"), found.total());
        assertEquals(PurchaseStatus.COMPRADA, found.status());
        assertEquals(List.of("Inception", "Matrix"), found.lines().stream().map(PurchaseLineDto::title).toList());
        assertEquals(12L, found.lines().get(0).movieId());
        assertEquals(new BigDecimal("9.49"), found.lines().get(0).unitPrice());
        assertTrue(found.lines().get(0).id() < found.lines().get(1).id());
    }

    @Test
    void purchaseWithoutLinesIsReadWithAnEmptyList() {
        PurchaseDto saved = repository.save(purchase(1L, PURCHASED_AT));

        assertEquals(List.of(), repository.findByIdAndUserId(saved.id(), 1L).orElseThrow().lines());
        assertEquals(List.of(), repository.findByUserIdOrderByPurchasedAtDesc(1L).get(0).lines());
    }

    @Test
    void historyListsOnlyTheUsersPurchasesByDateDescendingEachWithItsOwnLines() {
        PurchaseDto old = repository.save(purchase(1L, LocalDateTime.of(2026, 11, 1, 9, 0), line(10L, "Matrix", "7.99")));
        PurchaseDto recent = repository.save(purchase(1L, LocalDateTime.of(2026, 11, 3, 9, 0),
                line(12L, "Inception", "9.49"), line(13L, "Alien", "12.99")));
        PurchaseDto intermediate = repository.save(purchase(1L, LocalDateTime.of(2026, 11, 2, 9, 0)));
        repository.save(purchase(2L, LocalDateTime.of(2026, 11, 4, 9, 0), line(99L, "Other", "1.00")));

        List<PurchaseDto> history = repository.findByUserIdOrderByPurchasedAtDesc(1L);

        assertEquals(List.of(recent, intermediate, old), history);
        assertEquals(List.of("Inception", "Alien"), history.get(0).lines().stream().map(PurchaseLineDto::title).toList());
        assertEquals(List.of(), history.get(1).lines());
        assertEquals(List.of("Matrix"), history.get(2).lines().stream().map(PurchaseLineDto::title).toList());
    }

    @Test
    void purchasesOfTheSameInstantAreListedByIdDescending() {
        PurchaseDto first = repository.save(purchase(1L, PURCHASED_AT));
        PurchaseDto second = repository.save(purchase(1L, PURCHASED_AT));

        assertEquals(List.of(second.id(), first.id()),
                repository.findByUserIdOrderByPurchasedAtDesc(1L).stream().map(PurchaseDto::id).toList());
    }

    @Test
    void historyOfUserWithoutPurchasesIsEmpty() {
        repository.save(purchase(2L, PURCHASED_AT, line(10L, "Matrix", "7.99")));

        assertTrue(repository.findByUserIdOrderByPurchasedAtDesc(1L).isEmpty());
    }

    @Test
    void othersOrUnknownPurchaseIsNotFoundByUser() {
        PurchaseDto others = repository.save(purchase(2L, PURCHASED_AT, line(10L, "Matrix", "7.99")));

        assertTrue(repository.findByIdAndUserId(others.id(), 1L).isEmpty());
        assertTrue(repository.findByIdAndUserId(987654L, 2L).isEmpty());
        assertEquals(others, repository.findByIdAndUserId(others.id(), 2L).orElseThrow());
    }

    @Test
    void linesKeepTitleAndPriceEvenIfTheMovieIsUpdatedOrDeleted() {
        jdbc.sql("insert into peliculas (id, titulo, precio) values (500, 'Matrix', 7.99)").update();
        Long id = repository.save(purchase(1L, PURCHASED_AT, line(500L, "Matrix", "7.99"))).id();

        jdbc.sql("update peliculas set titulo = 'Matrix Reloaded', precio = 14.99 where id = 500").update();

        PurchaseLineDto afterUpdate = repository.findByIdAndUserId(id, 1L).orElseThrow().lines().get(0);
        assertEquals("Matrix", afterUpdate.title());
        assertEquals(new BigDecimal("7.99"), afterUpdate.unitPrice());

        jdbc.sql("delete from peliculas where id = 500").update();

        List<PurchaseLineDto> afterDelete = repository.findByIdAndUserId(id, 1L).orElseThrow().lines();
        assertEquals(1, afterDelete.size());
        assertEquals("Matrix", afterDelete.get(0).title());
        assertEquals(new BigDecimal("7.99"), afterDelete.get(0).unitPrice());
        assertEquals(500L, afterDelete.get(0).movieId());
    }

    /**
     * The date is not checked here: MariaDB fills a null in the first {@code TIMESTAMP NOT NULL} column of a table
     * with the current instant instead of rejecting it, and the service always sets it.
     */
    @Test
    void purchaseWithoutMandatoryColumnIsRejectedAndLeavesNoRow() {
        int purchasesBefore = count("compras");
        int linesBefore = count("lineas_compra");

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(new PurchaseDto(
                null, null, PURCHASED_AT, new BigDecimal("7.99"), PurchaseStatus.COMPRADA,
                List.of(line(10L, "Matrix", "7.99")))));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(new PurchaseDto(
                null, 1L, PURCHASED_AT, new BigDecimal("7.99"), null, List.of(line(10L, "Matrix", "7.99")))));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(new PurchaseDto(
                null, 1L, PURCHASED_AT, null, PurchaseStatus.COMPRADA, List.of(line(10L, "Matrix", "7.99")))));

        assertEquals(purchasesBefore, count("compras"));
        assertEquals(linesBefore, count("lineas_compra"));
    }

    @Test
    void lineWithoutTitleOrPriceIsRejected() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(
                purchase(1L, PURCHASED_AT, new PurchaseLineDto(null, null, 10L, null, new BigDecimal("7.99")))));
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(
                purchase(1L, PURCHASED_AT, new PurchaseLineDto(null, null, 10L, "Matrix", null))));
    }
}
