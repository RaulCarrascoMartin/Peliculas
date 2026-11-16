package shop.orders.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.model.Order;
import shop.orders.internal.domain.model.OrderLine;
import shop.orders.internal.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        var dto = new OrderDto(10L, 7L, OrderStatus.CREATED, Instant.parse("2026-11-02T10:15:30Z"),
                new BigDecimal("33.48"), List.of(
                        new OrderLineDto(1L, "Alien", new BigDecimal("12.99"), 2),
                        new OrderLineDto(2L, "Matrix", new BigDecimal("7.50"), 1)));

        Order model = mapper.toModel(dto);

        assertEquals(10L, model.getId());
        assertEquals(7L, model.getUserId());
        assertEquals(OrderStatus.CREATED, model.getStatus());
        assertEquals(2, model.getLines().size());
        assertEquals("Matrix", model.getLines().get(1).getTitle());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void reservedItemBecomesLineAndLineBecomesStockItem() {
        OrderLine line = mapper.toLine(new ReservedItemDto(1L, "Alien", new BigDecimal("12.99"), 2));

        assertEquals(1L, line.getProductId());
        assertEquals("Alien", line.getTitle());
        assertEquals(new BigDecimal("12.99"), line.getUnitPrice());
        assertEquals(2, line.getQuantity());
        assertEquals(new StockItemDto(1L, 2), mapper.toStockItem(line));
        assertEquals(List.of(new StockItemDto(1L, 2)), mapper.toStockItems(List.of(line)));
    }
}
