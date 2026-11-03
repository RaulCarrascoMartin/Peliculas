package shop.orders.internal.application.usecase;

import shop.orders.internal.application.dto.NewOrder;
import shop.orders.internal.application.dto.NewOrderLine;
import shop.orders.internal.application.dto.OrderView;
import shop.orders.internal.domain.fake.OrderRepositoryFake;
import shop.orders.internal.domain.fake.StockReservationFake;

import shop.orders.internal.domain.service.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Isolated orders module: the catalog API is hand-doubled. */
class OrdersUseCasesTest {

    private StockReservationFake catalog;
    private OrderRepositoryFake orders;
    private CreateOrder create;
    private GetOrder query;
    private CancelOrder cancel;

    @BeforeEach
    void setUp() {
        catalog = new StockReservationFake();
        catalog.createProduct(1L, "Alien", "12.99", 5);
        catalog.createProduct(2L, "Matrix", "7.50", 2);
        orders = new OrderRepositoryFake();
        var service = new OrderServiceImpl(catalog, orders);
        create = new CreateOrder(service);
        query = new GetOrder(service);
        cancel = new CancelOrder(service);
    }

    private static NewOrder quantities(NewOrderLine... lines) {
        return new NewOrder(7L, List.of(lines));
    }

    @Test
    void createReservesStockAndPersistsOrderWithCapturedPrices() {
        OrderView view = create.execute(quantities(new NewOrderLine(1L, 2), new NewOrderLine(2L, 1)));

        assertNotNull(view.id());
        assertEquals("CREATED", view.status());
        assertEquals(7L, view.userId());
        assertEquals(3, catalog.stockOf(1L));
        assertEquals(1, catalog.stockOf(2L));
        assertEquals(0, new BigDecimal("33.48").compareTo(view.total()));
        assertEquals(2, view.lines().size());
        assertEquals(0, new BigDecimal("12.99").compareTo(view.lines().get(0).unitPrice()));
        assertEquals("Alien", view.lines().get(0).title());
        assertEquals(view, query.execute(view.id()));
    }

    @Test
    void capturedPriceDoesNotChangeIfCatalogChangesLater() {
        OrderView view = create.execute(quantities(new NewOrderLine(1L, 1)));

        catalog.changePrice(1L, "99.00");

        assertEquals(0, new BigDecimal("12.99").compareTo(query.execute(view.id()).lines().get(0).unitPrice()));
    }

    @Test
    void lineWithoutStockLeavesNothingReservedAndCreatesNoOrder() {
        assertThrows(ConflictException.class,
                () -> create.execute(quantities(new NewOrderLine(1L, 2), new NewOrderLine(2L, 3))));

        assertEquals(5, catalog.stockOf(1L));
        assertEquals(2, catalog.stockOf(2L));
        assertEquals(0, orders.count());
    }

    @Test
    void unknownProductGives404AndReservesNothing() {
        assertThrows(ResourceNotFoundException.class,
                () -> create.execute(quantities(new NewOrderLine(1L, 1), new NewOrderLine(99L, 1))));

        assertEquals(5, catalog.stockOf(1L));
        assertEquals(0, orders.count());
    }

    @Test
    void gettingUnknownOrderReturns404() {
        assertThrows(ResourceNotFoundException.class, () -> query.execute(99L));
    }

    @Test
    void cancelReleasesStockAndMovesToCancelled() {
        OrderView view = create.execute(quantities(new NewOrderLine(1L, 2), new NewOrderLine(2L, 2)));

        OrderView cancelled = cancel.execute(view.id());

        assertEquals("CANCELLED", cancelled.status());
        assertEquals("CANCELLED", query.execute(view.id()).status());
        assertEquals(5, catalog.stockOf(1L));
        assertEquals(2, catalog.stockOf(2L));
    }

    @Test
    void cancellingTwiceFailsWithConflictAndDoesNotReleaseAgain() {
        OrderView view = create.execute(quantities(new NewOrderLine(1L, 2)));
        cancel.execute(view.id());

        assertThrows(ConflictException.class, () -> cancel.execute(view.id()));

        assertEquals(5, catalog.stockOf(1L));
    }

    @Test
    void cancellingUnknownOrderReturns404() {
        assertThrows(ResourceNotFoundException.class, () -> cancel.execute(99L));
    }
}
