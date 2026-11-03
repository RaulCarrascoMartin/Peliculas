package shop.orders.internal.domain.service;

import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.model.Order;
import shop.orders.internal.domain.model.OrderLine;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderServiceImpl implements OrderService {

    private final StockReservation stock;
    private final OrderRepository orders;

    public OrderServiceImpl(StockReservation stock, OrderRepository orders) {
        this.stock = stock;
        this.orders = orders;
    }

    @Override
    public Order create(Long userId, List<StockReservation.Item> items) {
        List<StockReservation.Reserved> reserved = stock.reserve(items);
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setLines(reserved.stream().map(OrderServiceImpl::toOrderLine).toList());
        order.setTotal(order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return orders.save(order);
    }

    @Override
    public Order find(Long id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public Order cancel(Long id) {
        Order order = find(id);
        if (order.getStatus() != OrderStatus.CREATED || !orders.cancelIfCreated(id)) {
            throw new OrderCannotBeCancelledException(id);
        }
        stock.release(order.getLines().stream()
                .map(l -> new StockReservation.Item(l.getProductId(), l.getQuantity()))
                .toList());
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    private static OrderLine toOrderLine(StockReservation.Reserved reserved) {
        OrderLine line = new OrderLine();
        line.setProductId(reserved.productId());
        line.setTitle(reserved.title());
        line.setUnitPrice(reserved.unitPrice());
        line.setQuantity(reserved.quantity());
        return line;
    }
}
