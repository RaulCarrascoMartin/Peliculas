package shop.orders.internal.domain.service;

import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;
import shop.orders.internal.domain.model.Order;

import java.util.List;

/** Order rules: reserve the stock, capture prices and total, and cancel only from CREATED. */
public interface OrderService {

    /**
     * Reserves the stock of all lines (which captures the price at that moment) and saves the order. There is no
     * transaction in this milestone: if saving fails after reserving, the stock stays reserved without an order.
     */
    Order create(Long userId, List<StockReservation.Item> items);

    /** @throws OrderNotFoundException if it does not exist */
    Order find(Long id);

    /**
     * Only cancellable from CREATED. The status change is conditional: if two cancellations coincide, only one
     * succeeds and only that one returns the stock.
     *
     * @throws OrderNotFoundException         if it does not exist
     * @throws OrderCannotBeCancelledException if it is not CREATED
     */
    Order cancel(Long id);
}
