package shop.catalog.internal.application.usecase;

import shop.catalog.api.facade.dto.StockLineDto;
import shop.catalog.internal.domain.service.StockService;
import shop.catalog.internal.domain.service.StockService.StockItem;

import java.util.List;

public class ReleaseStock {

    private final StockService stock;

    public ReleaseStock(StockService stock) {
        this.stock = stock;
    }

    /** Returns the units to the stock; a product that no longer exists is ignored. */
    public void execute(List<StockLineDto> lines) {
        stock.release(lines.stream().map(l -> new StockItem(l.productId(), l.quantity())).toList());
    }
}
