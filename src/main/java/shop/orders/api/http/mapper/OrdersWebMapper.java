package shop.orders.api.http.mapper;

import shop.orders.api.http.dto.Order;
import shop.orders.api.http.dto.OrderLine;
import shop.orders.internal.application.dto.OrderLineView;
import shop.orders.internal.application.dto.OrderView;

import java.time.ZoneOffset;
import java.util.List;

/** Converts the use case views into the DTO of the HTTP contract. */
public final class OrdersWebMapper {

    private OrdersWebMapper() {
    }

    public static Order toDto(OrderView view) {
        List<OrderLine> lines = view.lines().stream().map(OrdersWebMapper::toDto).toList();
        return new Order(view.id(), view.userId(), Order.StatusEnum.valueOf(view.status()),
                view.createdAt().atOffset(ZoneOffset.UTC), view.total(), lines);
    }

    public static OrderLine toDto(OrderLineView line) {
        return new OrderLine(line.productId(), line.title(), line.unitPrice(), line.quantity());
    }
}
