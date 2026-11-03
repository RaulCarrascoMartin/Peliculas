package shop.orders.internal.domain.service;

import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.model.OrderStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.orders.internal.domain.fake.OrderRepositoryFake;
import shop.orders.internal.domain.fake.StockReservationFake;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private StockReservationFake stock;
    private OrderService service;

    @BeforeEach
    void setUp() {
        stock = new StockReservationFake();
        stock.createProduct(1L, "Alien", "10.00", 5);
        service = new OrderServiceImpl(stock, new OrderRepositoryFake());
    }

    @Test
    void createCapturesPricesAndComputesTotal() {
        var order = service.create(7L, List.of(new StockReservation.Item(1L, 3)));
        stock.changePrice(1L, "99.00");

        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(0, new BigDecimal("30.00").compareTo(service.find(order.getId()).getTotal()));
        assertEquals(2, stock.stockOf(1L));
    }

    @Test
    void cancelReturnsStockOnlyOnce() {
        var order = service.create(7L, List.of(new StockReservation.Item(1L, 3)));

        assertEquals(OrderStatus.CANCELLED, service.cancel(order.getId()).getStatus());
        assertEquals(5, stock.stockOf(1L));
        assertThrows(OrderCannotBeCancelledException.class, () -> service.cancel(order.getId()));
        assertEquals(5, stock.stockOf(1L));
    }

    @Test
    void findingOrCancellingUnknownOrderFails() {
        assertThrows(OrderNotFoundException.class, () -> service.find(99L));
        assertThrows(OrderNotFoundException.class, () -> service.cancel(99L));
    }
}
