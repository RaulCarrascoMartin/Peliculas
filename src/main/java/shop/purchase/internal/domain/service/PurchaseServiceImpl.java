package shop.purchase.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.purchase.internal.domain.contract.MovieCatalog;
import shop.purchase.internal.domain.contract.UserCart;
import shop.purchase.internal.domain.dto.CatalogMovieDto;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;
import shop.purchase.internal.domain.mapper.PurchaseMapper;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchases;
    private final UserCart carts;
    private final MovieCatalog movies;
    private final PurchaseMapper mapper;

    public PurchaseServiceImpl(PurchaseRepository purchases, UserCart carts, MovieCatalog movies,
                               PurchaseMapper mapper) {
        this.purchases = purchases;
        this.carts = carts;
        this.movies = movies;
        this.mapper = mapper;
    }

    /**
     * One transaction for saving the purchase and clearing the cart: the cart's optimistic lock fires at commit and
     * undoes the purchase of the checkout that loses the race.
     */
    @Override
    @Transactional
    public PurchaseDto checkout(Long userId) {
        List<Long> ids = carts.findMovieIds(userId);
        if (ids.isEmpty()) {
            throw new EmptyCartException();
        }
        Map<Long, CatalogMovieDto> byId = movies.findByIds(ids).stream()
                .collect(Collectors.toMap(CatalogMovieDto::id, Function.identity()));
        List<CatalogMovieDto> purchasable = ids.stream().map(id -> purchasable(byId.get(id), id)).toList();

        Purchase purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setPurchasedAt(LocalDateTime.now());
        purchase.setStatus(PurchaseStatus.COMPRADA);
        purchase.setLines(mapper.toLines(purchasable));
        purchase.setTotal(purchase.getLines().stream()
                .map(PurchaseLine::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PurchaseDto saved = purchases.save(mapper.toDto(purchase));
        carts.clear(userId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseDto> historyOf(Long userId) {
        return purchases.findByUserIdOrderByPurchasedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseDto detailOf(Long userId, Long purchaseId) {
        return purchases.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private static CatalogMovieDto purchasable(CatalogMovieDto movie, Long movieId) {
        if (movie == null) {
            throw new MovieNoLongerAvailableException(movieId);
        }
        if (movie.price() == null) {
            throw new MovieWithoutPriceException(movieId);
        }
        return movie;
    }
}
