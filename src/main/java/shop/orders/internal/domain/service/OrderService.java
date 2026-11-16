package shop.orders.internal.domain.service;

import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.exception.OrderCannotBeCancelledException;
import shop.orders.internal.domain.exception.OrderNotFoundException;

/** Order rules: reserve the stock, capture prices and total, and cancel only from CREATED. */
public interface OrderService {

    /**
     * Reserves the stock of all lines (which captures the price at that moment) and saves the order, in one
     * transaction: if saving fails, the reserved stock is released. The failures of the reservation are those of
     * {@link StockReservation#reserve}.
     */
    OrderDto create(CreateOrderDto request);

    /** @throws OrderNotFoundException if it does not exist or belongs to another user */
    OrderDto find(Long userId, Long id);

    /**
     * Only cancellable from CREATED. The status change is conditional: if two cancellations coincide, only one
     * succeeds and only that one returns the stock.
     *
     * @throws OrderNotFoundException         if it does not exist or belongs to another user
     * @throws OrderCannotBeCancelledException if it is not CREATED
     */
    OrderDto cancel(Long userId, Long id);
}
