package shop.orders.internal.domain.contract;

import java.math.BigDecimal;
import java.util.List;

/**
 * What orders needs from the catalog: reserve and release stock. The adapter in {@code persistence} fulfils it and
 * translates the catalog's failures into the common business exceptions (404 and 409).
 */
public interface StockReservation {

    /** Quantity of a product to reserve or release. */
    record Item(Long productId, int quantity) {
    }

    /** Reserved product with the title and unit price captured at reservation time. */
    record Reserved(Long productId, String title, BigDecimal unitPrice, int quantity) {
    }

    /**
     * Reserves all the lines or none.
     *
     * @throws shop.shared.error.ResourceNotFoundException if a product does not exist
     * @throws shop.shared.error.ConflictException         if a product is not purchasable or lacks stock
     */
    List<Reserved> reserve(List<Item> items);

    /** Returns the units to the stock. */
    void release(List<Item> items);
}
