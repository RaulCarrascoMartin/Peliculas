package shop.purchase.internal.domain.service;

import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;

import java.util.List;

/** Purchase rules: checkout of the cart with a title and price snapshot, and the user's own history. */
public interface PurchaseService {

    /** A purchase with its frozen lines. */
    record PurchaseDetail(Purchase purchase, List<PurchaseLine> lines) {
    }

    /**
     * Converts the user's cart into a purchase and clears the cart. Everything is validated before writing, so a
     * conflict leaves the cart intact.
     *
     * @throws EmptyCartException              if the cart has no lines
     * @throws MovieNoLongerAvailableException if a movie left the catalog
     * @throws MovieWithoutPriceException      if a movie has no price
     */
    PurchaseDetail checkout(Long userId);

    /** The user's own purchases, from most recent to oldest. */
    List<PurchaseDetail> historyOf(Long userId);

    /** @throws PurchaseNotFoundException if it does not exist or belongs to another user */
    PurchaseDetail detailOf(Long userId, Long purchaseId);
}
