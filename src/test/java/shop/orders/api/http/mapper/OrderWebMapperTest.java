package shop.orders.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.orders.api.http.dto.CreateOrderLineRequest;
import shop.orders.api.http.dto.CreateOrderRequest;
import shop.orders.api.http.dto.OrderLineResponse;
import shop.orders.api.http.dto.OrderResponse;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderWebMapperTest {

    private final OrderWebMapper mapper = Mappers.getMapper(OrderWebMapper.class);

    @Test
    void requestBecomesTheDtoOfTheService() {
        var request = new CreateOrderRequest(List.of(new CreateOrderLineRequest(1L, 2)));

        assertEquals(new CreateOrderDto(7L, List.of(new StockItemDto(1L, 2))), mapper.toDto(7L, request));
    }

    @Test
    void orderBecomesTheResponseOfTheContract() {
        var order = new OrderDto(10L, 7L, OrderStatus.CANCELLED, Instant.parse("2026-11-02T10:15:30Z"),
                new BigDecimal("25.98"), List.of(new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2)));

        OrderResponse response = mapper.toResponse(order);

        assertEquals(new OrderResponse(10L, 7L, OrderResponse.StatusEnum.CANCELLED,
                OffsetDateTime.parse("2026-11-02T10:15:30Z"), new BigDecimal("25.98"),
                List.of(new OrderLineResponse(1L, "Alien", new BigDecimal("12.99"), 2))), response);
    }
}
