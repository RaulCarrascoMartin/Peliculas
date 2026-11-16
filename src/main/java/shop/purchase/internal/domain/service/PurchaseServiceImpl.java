package shop.purchase.internal.domain.service;

import shop.purchase.internal.domain.contract.CatalogMovie;
import shop.purchase.internal.domain.contract.MovieCatalog;
import shop.purchase.internal.domain.contract.UserCart;
import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchases;
    private final PurchaseLineRepository lines;
    private final UserCart carts;
    private final MovieCatalog movies;

    public PurchaseServiceImpl(PurchaseRepository purchases, PurchaseLineRepository lines, UserCart carts,
                               MovieCatalog movies) {
        this.purchases = purchases;
        this.lines = lines;
        this.carts = carts;
        this.movies = movies;
    }

    @Override
    public PurchaseDetail checkout(Long userId) {
        List<Long> ids = carts.findMovieIds(userId);
        if (ids.isEmpty()) {
            throw new EmptyCartException();
        }
        Map<Long, CatalogMovie> byId = movies.findByIds(ids).stream()
                .collect(Collectors.toMap(CatalogMovie::id, Function.identity()));
        List<CatalogMovie> purchasable = ids.stream().map(id -> purchasable(byId.get(id), id)).toList();

        var purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setPurchasedAt(LocalDateTime.now());
        purchase.setTotal(purchasable.stream().map(CatalogMovie::price).reduce(BigDecimal.ZERO, BigDecimal::add));
        purchase.setStatus(PurchaseStatus.COMPRADA);
        Purchase saved = purchases.save(purchase);

        List<PurchaseLine> frozen = purchasable.stream().map(m -> lines.save(snapshot(saved.getId(), m))).toList();
        carts.clear(userId);
        return new PurchaseDetail(saved, frozen);
    }

    @Override
    public List<PurchaseDetail> historyOf(Long userId) {
        return purchases.findByUserIdOrderByPurchasedAtDesc(userId).stream().map(this::withLines).toList();
    }

    @Override
    public PurchaseDetail detailOf(Long userId, Long purchaseId) {
        return purchases.findByIdAndUserId(purchaseId, userId)
                .map(this::withLines)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private PurchaseDetail withLines(Purchase purchase) {
        return new PurchaseDetail(purchase, lines.findByPurchaseIdOrderById(purchase.getId()));
    }

    private static CatalogMovie purchasable(CatalogMovie movie, Long movieId) {
        if (movie == null) {
            throw new MovieNoLongerAvailableException(movieId);
        }
        if (movie.price() == null) {
            throw new MovieWithoutPriceException(movieId);
        }
        return movie;
    }

    private static PurchaseLine snapshot(Long purchaseId, CatalogMovie movie) {
        var line = new PurchaseLine();
        line.setPurchaseId(purchaseId);
        line.setMovieId(movie.id());
        line.setTitle(movie.title());
        line.setUnitPrice(movie.price());
        return line;
    }
}
