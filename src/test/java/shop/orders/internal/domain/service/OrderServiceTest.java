package shop.orders.internal.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.exception.InsufficientStockException;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.exception.ProductNotFoundException;
import shop.orders.internal.domain.mapper.OrderMapper;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Order rules with the repository and the catalog mocked; the MapStruct mapper is the real one. */
class OrderServiceTest {

    private static final List<StockItemDto> ITEMS = List.of(new StockItemDto(1L, 2), new StockItemDto(2L, 1));

    private StockReservation stock;
    private OrderRepository orders;
    private OrderService service;

    @BeforeEach
    void setUp() {
        stock = mock(StockReservation.class);
        orders = mock(OrderRepository.class);
        service = new OrderServiceImpl(stock, orders, Mappers.getMapper(OrderMapper.class));
    }

    private static OrderDto stored(OrderStatus status) {
        return new OrderDto(10L, 7L, status, Instant.parse("2026-11-02T10:15:30Z"), new BigDecimal("25.98"),
                List.of(new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2)));
    }

    @Test
    void createReservesStockAndSavesOrderWithCapturedPricesAndTotal() {
        when(stock.reserve(ITEMS)).thenReturn(List.of(
                new ReservedItemDto(1L, "Alien", new BigDecimal("12.99"), 2),
                new ReservedItemDto(2L, "Matrix", new BigDecimal("7.50"), 1)));
        when(orders.save(any())).thenAnswer(call -> {
            OrderDto o = call.getArgument(0);
            return new OrderDto(10L, o.userId(), o.status(), o.createdAt(), o.total(), o.lines());
        });

        OrderDto created = service.create(new CreateOrderDto(7L, ITEMS));

        ArgumentCaptor<OrderDto> saved = ArgumentCaptor.forClass(OrderDto.class);
        verify(orders).save(saved.capture());
        assertNull(saved.getValue().id());
        assertEquals(7L, saved.getValue().userId());
        assertEquals(OrderStatus.CREATED, saved.getValue().status());
        assertNotNull(saved.getValue().createdAt());
        assertEquals(new BigDecimal("33.48"), saved.getValue().total());
        assertEquals(List.of(
                new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2),
                new OrderLineDto(2L, "Matrix", new BigDecimal("7.50"), 1)), saved.getValue().lines());
        assertEquals(10L, created.id());
    }

    @Test
    void failureWhileSavingReleasesTheReservedStock() {
        when(stock.reserve(ITEMS)).thenReturn(List.of(
                new ReservedItemDto(1L, "Alien", new BigDecimal("12.99"), 2),
                new ReservedItemDto(2L, "Matrix", new BigDecimal("7.50"), 1)));
        when(orders.save(any())).thenThrow(new IllegalStateException("insert failed"));

        assertThrows(IllegalStateException.class, () -> service.create(new CreateOrderDto(7L, ITEMS)));

        verify(stock).release(ITEMS);
    }

    @Test
    void anotherUsersOrderIsReportedAsMissingAndNeverCancelled() {
        when(orders.findById(10L)).thenReturn(Optional.of(stored(OrderStatus.CREATED)));

        assertThrows(OrderNotFoundException.class, () -> service.find(8L, 10L));
        assertThrows(OrderNotFoundException.class, () -> service.cancel(8L, 10L));

        verify(orders, never()).cancelIfCreated(any());
        verify(stock, never()).release(any());
    }

    @Test
    void failedReservationCreatesNoOrder() {
        when(stock.reserve(ITEMS)).thenThrow(new InsufficientStockException(2L));

        assertThrows(InsufficientStockException.class, () -> service.create(new CreateOrderDto(7L, ITEMS)));

        verify(orders, never()).save(any());
    }

    @Test
    void unknownProductCreatesNoOrder() {
        when(stock.reserve(ITEMS)).thenThrow(new ProductNotFoundException(2L));

        var ex = assertThrows(ProductNotFoundException.class, () -> service.create(new CreateOrderDto(7L, ITEMS)));

        assertTrue(ex.getMessage().contains("2"));
        verify(orders, never()).save(any());
    }

    @Test
    void findReturnsTheStoredOrder() {
        when(orders.findById(10L)).thenReturn(Optional.of(stored(OrderStatus.CREATED)));

        assertEquals(stored(OrderStatus.CREATED), service.find(7L, 10L));
    }

    @Test
    void findingOrCancellingUnknownOrderFails() {
        when(orders.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(OrderNotFoundException.class, () -> service.find(7L, 99L));
        assertTrue(ex.getMessage().contains("99"));
        assertThrows(OrderNotFoundException.class, () -> service.cancel(7L, 99L));
        verify(orders, never()).cancelIfCreated(any());
    }

    @Test
    void cancelMovesToCancelledAndReleasesTheStockOfItsLines() {
        when(orders.findById(10L)).thenReturn(Optional.of(stored(OrderStatus.CREATED)));
        when(orders.cancelIfCreated(10L)).thenReturn(true);

        OrderDto cancelled = service.cancel(7L, 10L);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(stored(OrderStatus.CREATED).lines(), cancelled.lines());
        verify(stock).release(List.of(new StockItemDto(1L, 2)));
    }

    @Test
    void alreadyCancelledOrderCannotBeCancelledNorReleasesStock() {
        when(orders.findById(10L)).thenReturn(Optional.of(stored(OrderStatus.CANCELLED)));

        assertThrows(OrderCannotBeCancelledException.class, () -> service.cancel(7L, 10L));

        verify(orders, never()).cancelIfCreated(any());
        verify(stock, never()).release(any());
    }

    @Test
    void cancellationThatLosesTheRaceDoesNotReleaseStock() {
        when(orders.findById(10L)).thenReturn(Optional.of(stored(OrderStatus.CREATED)));
        when(orders.cancelIfCreated(10L)).thenReturn(false);

        assertThrows(OrderCannotBeCancelledException.class, () -> service.cancel(7L, 10L));

        verify(stock, never()).release(any());
    }
}
