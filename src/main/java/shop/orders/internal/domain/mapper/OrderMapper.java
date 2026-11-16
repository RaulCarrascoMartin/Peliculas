package shop.orders.internal.domain.mapper;

import org.mapstruct.Mapper;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.model.Order;
import shop.orders.internal.domain.model.OrderLine;

import java.util.List;

/** DTO ↔ model of the orders domain. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    Order toModel(OrderDto dto);

    OrderDto toDto(Order order);

    /** A reserved product becomes an order line with the captured title and price. */
    OrderLine toLine(ReservedItemDto reserved);

    List<OrderLine> toLines(List<ReservedItemDto> reserved);

    /** What an order holds of each product, to give it back to the stock. */
    StockItemDto toStockItem(OrderLine line);

    List<StockItemDto> toStockItems(List<OrderLine> lines);
}
