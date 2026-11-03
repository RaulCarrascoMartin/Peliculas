package shop.purchase.internal.usecase;

import shop.cart.api.CartApi;
import shop.shared.error.ConflictException;
import shop.purchase.internal.domain.Purchase;
import shop.purchase.internal.domain.PurchaseRepository;
import shop.purchase.internal.domain.PurchaseStatus;
import shop.purchase.internal.domain.PurchaseLine;
import shop.purchase.internal.domain.PurchaseLineRepository;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Atomic checkout with a title and price snapshot. Reads and clears the cart only through {@link CartApi}
 * and reads the movies only through {@link CatalogApi}.
 */
@Service
public class CompletePurchase {

    private final PurchaseRepository purchases;
    private final PurchaseLineRepository lines;
    private final CartApi carts;
    private final CatalogApi movies;

    public CompletePurchase(PurchaseRepository purchases, PurchaseLineRepository lines, CartApi carts, CatalogApi movies) {
        this.purchases = purchases;
        this.lines = lines;
        this.carts = carts;
        this.movies = movies;
    }

    /**
     * Converts the cart into a purchase and clears it in the same transaction. Everything is validated before
     * writing, so a conflict leaves the cart intact. The cart's optimistic lock usually fires at commit,
     * outside this method, and {@code GlobalExceptionHandler} translates it; here it is only caught if the
     * flush happens inside.
     */
    @Transactional
    public PurchaseView execute(Long userId) {
        try {
            return createPurchase(userId);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("El carrito ya se está finalizando en otra petición");
        }
    }

    private PurchaseView createPurchase(Long userId) {
        List<Long> ids = carts.findMovieIdsInCart(userId);
        if (ids.isEmpty()) {
            throw new ConflictException("El carrito está vacío");
        }
        Map<Long, MovieSummary> summaryById = movies.findSummaries(ids).stream()
                .collect(Collectors.toMap(MovieSummary::id, Function.identity()));
        List<MovieSummary> purchasable = ids.stream().map(id -> purchasable(summaryById.get(id), id)).toList();

        BigDecimal total = purchasable.stream()
                .map(MovieSummary::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setPurchasedAt(LocalDateTime.now());
        purchase.setTotal(total);
        purchase.setStatus(PurchaseStatus.COMPRADA);
        Purchase saved = purchases.save(purchase);

        List<PurchaseLine> frozenLines = purchasable.stream()
                .map(summary -> lines.save(snapshot(saved.getId(), summary)))
                .toList();
        carts.clear(userId);
        return PurchaseViewMapper.toView(saved, frozenLines);
    }

    private static MovieSummary purchasable(MovieSummary summary, Long movieId) {
        if (summary == null) {
            throw new ConflictException("La película " + movieId + " ya no está en el catálogo");
        }
        if (summary.price() == null) {
            throw new ConflictException("La película " + movieId + " no tiene precio y no es comprable");
        }
        return summary;
    }

    private static PurchaseLine snapshot(Long purchaseId, MovieSummary summary) {
        var line = new PurchaseLine();
        line.setPurchaseId(purchaseId);
        line.setMovieId(summary.id());
        line.setTitle(summary.title());
        line.setUnitPrice(summary.price());
        return line;
    }
}
