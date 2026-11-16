package shop.orders.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.mapper.OrderMapper;
import shop.orders.internal.domain.model.Order;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * {@code create} and {@code cancel} run in a transaction: the stock is reserved or released through the catalog
 * facade, which joins it, and the order is saved with its lines in the same unit. If saving fails, the reservation is
 * also released explicitly so the stock never stays reserved without an order.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final StockReservation stock;
    private final OrderRepository orders;
    private final OrderMapper mapper;

    public OrderServiceImpl(StockReservation stock, OrderRepository orders, OrderMapper mapper) {
        this.stock = stock;
        this.orders = orders;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public OrderDto create(CreateOrderDto request) {
        List<ReservedItemDto> reserved = stock.reserve(request.lines());
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setLines(mapper.toLines(reserved));
        order.setTotal(order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        try {
            return orders.save(mapper.toDto(order));
        } catch (RuntimeException e) {
            stock.release(mapper.toStockItems(order.getLines()));
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto find(Long userId, Long id) {
        return mapper.toDto(load(userId, id));
    }

    @Override
    @Transactional
    public OrderDto cancel(Long userId, Long id) {
        Order order = load(userId, id);
        if (order.getStatus() != OrderStatus.CREATED || !orders.cancelIfCreated(id)) {
            throw new OrderCannotBeCancelledException(id);
        }
        stock.release(mapper.toStockItems(order.getLines()));
        order.setStatus(OrderStatus.CANCELLED);
        return mapper.toDto(order);
    }

    /** An order of another user is reported as missing, so its existence is not revealed. */
    private Order load(Long userId, Long id) {
        return orders.findById(id)
                .map(mapper::toModel)
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
