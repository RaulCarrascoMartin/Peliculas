package shop.orders.internal.persistence.adapter;

import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.orders.internal.domain.contract.StockReservation;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;

import java.util.List;

/** Translates the orders' {@link StockReservation} contract to the public facade of {@code catalog}. */
public class StockReservationAdapter implements StockReservation {

    private final CatalogFacade catalog;

    public StockReservationAdapter(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Reserved> reserve(List<Item> items) {
        try {
            return catalog.reserve(toLines(items)).stream()
                    .map(r -> new Reserved(r.productId(), r.title(), r.unitPrice(), r.quantity()))
                    .toList();
        } catch (UnknownProductException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (InsufficientStockException | ProductNotPurchasableException e) {
            throw new ConflictException(e.getMessage());
        }
    }

    @Override
    public void release(List<Item> items) {
        catalog.release(toLines(items));
    }

    private static List<StockLineDto> toLines(List<Item> items) {
        return items.stream().map(i -> new StockLineDto(i.productId(), i.quantity())).toList();
    }
}
