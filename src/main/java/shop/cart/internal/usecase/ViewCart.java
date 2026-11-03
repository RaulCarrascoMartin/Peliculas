package shop.cart.internal.usecase;

import shop.cart.internal.domain.Cart;
import shop.cart.internal.domain.CartRepository;
import shop.cart.internal.domain.CartLine;
import shop.cart.internal.domain.CartLineRepository;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The price is never persisted on the line: it is computed on read with the live price of the {@code movie}. */
@Service
public class ViewCart {

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final CatalogApi movies;

    public ViewCart(CartRepository carts, CartLineRepository lines, CatalogApi movies) {
        this.carts = carts;
        this.lines = lines;
        this.movies = movies;
    }

    public CartView execute(Long userId) {
        return carts.findByUserId(userId)
                .map(this::toView)
                .orElseGet(() -> new CartView(List.of(), BigDecimal.ZERO));
    }

    private CartView toView(Cart cart) {
        List<CartLine> storedLines = lines.findByCartIdOrderById(cart.getId());
        List<Long> ids = storedLines.stream().map(CartLine::getMovieId).toList();
        Map<Long, MovieSummary> summaryById = movies.findSummaries(ids).stream()
                .collect(Collectors.toMap(MovieSummary::id, Function.identity()));
        // Defence: a line whose movie no longer exists is skipped (summaries ignore unknown ids).
        List<CartLineView> currentLines = storedLines.stream()
                .filter(line -> summaryById.containsKey(line.getMovieId()))
                .map(line -> {
                    MovieSummary summary = summaryById.get(line.getMovieId());
                    return new CartLineView(line.getMovieId(), summary.title(), summary.price());
                })
                .toList();
        BigDecimal total = currentLines.stream()
                .map(CartLineView::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(currentLines, total);
    }
}
