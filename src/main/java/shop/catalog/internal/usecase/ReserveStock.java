package shop.catalog.internal.usecase;

import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.ProductNotPurchasableException;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;
import shop.catalog.internal.domain.Movie;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserves all the lines or none, without a transaction: first validates everything against the data read and
 * then deducts each product with a conditional subtraction; if one of those subtractions loses a race,
 * it gives back what was already deducted before failing.
 */
@Service
public class ReserveStock {

    private final MovieRepository movies;

    public ReserveStock(MovieRepository movies) {
        this.movies = movies;
    }

    public List<ReservedLine> execute(List<StockLine> lines) {
        Map<Long, Integer> quantityByProduct = groupByProduct(lines);
        Map<Long, Movie> existing = movies.findAllById(quantityByProduct.keySet()).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        validate(quantityByProduct, existing);
        deduct(quantityByProduct);

        return lines.stream()
                .map(l -> {
                    Movie p = existing.get(l.productId());
                    return new ReservedLine(p.getId(), p.getTitle(), p.getPrice(), l.quantity());
                })
                .toList();
    }

    /** Adds up the quantities of lines that repeat a product, keeping the order of appearance. */
    private static Map<Long, Integer> groupByProduct(List<StockLine> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        lines.forEach(l -> quantities.merge(l.productId(), l.quantity(), Integer::sum));
        return quantities;
    }

    private static void validate(Map<Long, Integer> quantities, Map<Long, Movie> existing) {
        quantities.forEach((id, quantity) -> {
            Movie p = existing.get(id);
            if (p == null) {
                throw new UnknownProductException(id);
            }
            if (p.getPrice() == null) {
                throw new ProductNotPurchasableException(id);
            }
            if (p.getStock() < quantity) {
                throw new InsufficientStockException(id);
            }
        });
    }

    private void deduct(Map<Long, Integer> quantities) {
        List<Map.Entry<Long, Integer>> applied = new ArrayList<>();
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            if (movies.decrementStock(line.getKey(), line.getValue()) == 0) {
                applied.forEach(a -> movies.incrementStock(a.getKey(), a.getValue()));
                throw new InsufficientStockException(line.getKey());
            }
            applied.add(line);
        }
    }
}
