package shop.purchase.internal.domain.service;

import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.exception.EmptyCartException;
import shop.purchase.internal.domain.exception.MovieNoLongerAvailableException;
import shop.purchase.internal.domain.exception.MovieWithoutPriceException;
import shop.purchase.internal.domain.exception.PurchaseNotFoundException;

import java.util.List;

/** Purchase rules: checkout of the cart with a title and price snapshot, and the user's own history. */
public interface PurchaseService {

    /**
     * Converts the user's cart into a purchase and clears the cart in the same transaction. Everything is validated
     * before writing, so a conflict leaves the cart intact. A lost optimistic lock of the cart (double simultaneous
     * checkout) is not translated here: it propagates, the transaction rolls back and
     * {@code GlobalExceptionHandler} answers 409.
     *
     * @throws EmptyCartException              if the cart has no lines
     * @throws MovieNoLongerAvailableException if a movie left the catalog
     * @throws MovieWithoutPriceException      if a movie has no price
     */
    PurchaseDto checkout(Long userId);

    /** The user's own purchases, from most recent to oldest. */
    List<PurchaseDto> historyOf(Long userId);

    /**
     * Someone else's purchase is indistinguishable from a non-existent one: 404, never 403.
     *
     * @throws PurchaseNotFoundException if it does not exist or belongs to another user
     */
    PurchaseDto detailOf(Long userId, Long purchaseId);
}
