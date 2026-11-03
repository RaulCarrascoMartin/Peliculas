package shop.catalog.internal.usecase;

import shop.catalog.api.StockLine;
import shop.catalog.internal.domain.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReleaseStock {

    private final MovieRepository movies;

    public ReleaseStock(MovieRepository movies) {
        this.movies = movies;
    }

    /** Returns the units to the stock; a product that no longer exists affects no row and is ignored. */
    public void execute(List<StockLine> lines) {
        lines.forEach(l -> movies.incrementStock(l.productId(), l.quantity()));
    }
}
