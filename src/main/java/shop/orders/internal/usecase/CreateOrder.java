package shop.orders.internal.usecase;

import org.springframework.stereotype.Service;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.ProductNotPurchasableException;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;
import shop.orders.internal.domain.Order;
import shop.orders.internal.domain.OrderLine;
import shop.orders.internal.domain.OrderRepository;
import shop.orders.internal.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Reserves the stock of all lines through {@link CatalogApi} (which captures the price at that moment) and saves
 * the order. There is no transaction in this milestone: if saving fails after reserving, the stock stays
 * reserved without an order; this will be solved in milestone 07. Catalog errors are translated here to the
 * common business ones (404 and 409); catalog exceptions do not leave this use case.
 */
@Service
public class CreateOrder {

    private final CatalogApi catalog;
    private final OrderRepository orders;

    public CreateOrder(CatalogApi catalog, OrderRepository orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    public OrderView execute(NewOrder request) {
        List<ReservedLine> reserved = reserve(request.lines());
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setLines(reserved.stream().map(CreateOrder::toOrderLine).toList());
        order.setTotal(order.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return OrderView.of(orders.save(order));
    }

    private List<ReservedLine> reserve(List<NewOrderLine> lines) {
        try {
            return catalog.reserve(lines.stream().map(l -> new StockLine(l.productId(), l.quantity())).toList());
        } catch (UnknownProductException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (InsufficientStockException | ProductNotPurchasableException e) {
            throw new ConflictException(e.getMessage());
        }
    }

    private static OrderLine toOrderLine(ReservedLine reserved) {
        OrderLine line = new OrderLine();
        line.setProductId(reserved.productId());
        line.setTitle(reserved.title());
        line.setUnitPrice(reserved.unitPrice());
        line.setQuantity(reserved.quantity());
        return line;
    }
}
