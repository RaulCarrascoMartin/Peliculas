package shop.catalog.internal.application.usecase;

import shop.catalog.api.facade.exception.InsufficientStockException;
import shop.catalog.api.facade.exception.ProductNotPurchasableException;
import shop.catalog.api.facade.dto.ReservedLineDto;
import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.api.facade.exception.UnknownProductException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.service.StockService;
import shop.catalog.internal.domain.service.StockService.StockItem;

import java.util.List;

/** Reserves all the lines or none; the domain failures leave the module as the facade's exceptions. */
public class ReserveStock {

    private final StockService stock;

    public ReserveStock(StockService stock) {
        this.stock = stock;
    }

    public List<ReservedLineDto> execute(List<StockLineDto> lines) {
        try {
            return stock.reserve(lines.stream().map(l -> new StockItem(l.productId(), l.quantity())).toList()).stream()
                    .map(r -> new ReservedLineDto(r.movieId(), r.title(), r.unitPrice(), r.quantity()))
                    .toList();
        } catch (MovieNotFoundException e) {
            throw new UnknownProductException(e.movieId());
        } catch (MovieNotPurchasableException e) {
            throw new ProductNotPurchasableException(e.movieId());
        } catch (shop.catalog.internal.domain.exception.InsufficientStockException e) {
            throw new InsufficientStockException(e.movieId());
        }
    }
}
