package shop.orders.internal.domain.contract;

import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.exception.InsufficientStockException;
import shop.orders.internal.domain.exception.ProductNotFoundException;
import shop.orders.internal.domain.exception.ProductNotPurchasableException;

import java.util.List;

/**
 * What orders needs from the catalog: reserve and release stock. The adapter in {@code persistence} fulfils it and
 * translates the catalog's failures into the exceptions of this domain.
 */
public interface StockReservation {

    /**
     * Reserves all the lines or none.
     *
     * @throws ProductNotFoundException       if a product does not exist
     * @throws ProductNotPurchasableException if a product has no price
     * @throws InsufficientStockException     if there are not enough units of a product
     */
    List<ReservedItemDto> reserve(List<StockItemDto> items);

    /** Returns the units to the stock. */
    void release(List<StockItemDto> items);
}
