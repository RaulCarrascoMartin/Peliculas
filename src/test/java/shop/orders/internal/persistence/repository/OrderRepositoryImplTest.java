package shop.orders.internal.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.repository.OrderRepository;
import shop.shared.MariaDbRepositoryTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SQL and row ↔ DTO mapping of orders against a real MariaDB. */
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest extends MariaDbRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-02T10:15:30Z");

    @Autowired
    private OrderRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private static OrderDto order(OrderLineDto... lines) {
        return new OrderDto(null, 7L, OrderStatus.CREATED, CREATED_AT, new BigDecimal("33.48"), List.of(lines));
    }

    private static final OrderLineDto ALIEN = new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2);
    private static final OrderLineDto MATRIX = new OrderLineDto(2L, "Matrix", new BigDecimal("7.50"), 1);

    @Test
    void saveAssignsIdAndFindByIdReturnsEveryColumnAndTheLinesInOrder() {
        OrderDto saved = repository.save(order(ALIEN, MATRIX));

        assertNotNull(saved.id());
        OrderDto found = repository.findById(saved.id()).orElseThrow();
        assertEquals(saved, found);
        assertEquals(7L, found.userId());
        assertEquals(OrderStatus.CREATED, found.status());
        assertEquals(CREATED_AT, found.createdAt());
        assertEquals(new BigDecimal("33.48"), found.total());
        assertEquals(List.of(ALIEN, MATRIX), found.lines());
    }

    @Test
    void saveWithoutDateStoresTheCurrentInstant() {
        OrderDto saved = repository.save(
                new OrderDto(null, 7L, OrderStatus.CREATED, null, new BigDecimal("12.99"), List.of(ALIEN)));

        assertNotNull(saved.createdAt());
        assertNotNull(repository.findById(saved.id()).orElseThrow().createdAt());
    }

    @Test
    void findByIdOfUnknownOrderIsEmpty() {
        assertTrue(repository.findById(987654L).isEmpty());
    }

    @Test
    void linesOfOneOrderAreNotMixedWithThoseOfAnother() {
        OrderDto first = repository.save(order(ALIEN));
        OrderDto second = repository.save(order(MATRIX));

        assertEquals(List.of(ALIEN), repository.findById(first.id()).orElseThrow().lines());
        assertEquals(List.of(MATRIX), repository.findById(second.id()).orElseThrow().lines());
    }

    @Test
    void cancelIfCreatedChangesTheStatusOnlyOnce() {
        Long id = repository.save(order(ALIEN)).id();

        assertTrue(repository.cancelIfCreated(id));
        assertEquals(OrderStatus.CANCELLED, repository.findById(id).orElseThrow().status());
        assertFalse(repository.cancelIfCreated(id));
    }

    @Test
    void cancelIfCreatedOfUnknownOrderAffectsNothing() {
        assertFalse(repository.cancelIfCreated(987654L));
    }

    @Test
    void orderWithoutMandatoryColumnIsRejectedAndLeavesNoRow() {
        int before = jdbc.sql("select count(*) from orders").query(Integer.class).single();

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(
                new OrderDto(null, null, OrderStatus.CREATED, CREATED_AT, new BigDecimal("1.00"), List.of(ALIEN))));

        assertEquals(before, jdbc.sql("select count(*) from orders").query(Integer.class).single());
    }
}
