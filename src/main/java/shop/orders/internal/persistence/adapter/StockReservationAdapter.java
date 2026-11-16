package shop.orders.internal.persistence.adapter;

import jakarta.inject.Named;
import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.orders.internal.domain.contract.StockReservation;
import shop.orders.internal.domain.dto.ReservedItemDto;
import shop.orders.internal.domain.dto.StockItemDto;
import shop.orders.internal.domain.exception.InsufficientStockException;
import shop.orders.internal.domain.exception.ProductNotFoundException;
import shop.orders.internal.domain.exception.ProductNotPurchasableException;

import java.util.List;

/**
 * Translates the orders' {@link StockReservation} contract to the public facade of {@code catalog}: its types into
 * the DTO of this domain and its failures into the exceptions of this domain.
 */
@Named
public class StockReservationAdapter implements StockReservation {

    private final CatalogFacade catalog;

    public StockReservationAdapter(CatalogFacade catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<ReservedItemDto> reserve(List<StockItemDto> items) {
        try {
            return catalog.reserve(toLines(items)).stream()
                    .map(r -> new ReservedItemDto(r.productId(), r.title(), r.unitPrice(), r.quantity()))
                    .toList();
        } catch (UnknownProductException e) {
            throw new ProductNotFoundException(e.productId());
        } catch (shop.catalog.api.facade.exception.InsufficientStockException e) {
            throw new InsufficientStockException(e.productId());
        } catch (shop.catalog.api.facade.exception.ProductNotPurchasableException e) {
            throw new ProductNotPurchasableException(e.productId());
        }
    }

    @Override
    public void release(List<StockItemDto> items) {
        catalog.release(toLines(items));
    }

    private static List<StockLineDto> toLines(List<StockItemDto> items) {
        return items.stream().map(i -> new StockLineDto(i.productId(), i.quantity())).toList();
    }
}
