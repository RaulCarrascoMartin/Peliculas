package shop.purchase.internal.domain.repository;

import shop.purchase.internal.domain.dto.PurchaseDto;

import java.util.List;
import java.util.Optional;

/**
 * Purchase persistence; the implementation uses JDBC and lives in {@code internal.persistence}. The lines are
 * children of the purchase: they are saved and read with it.
 */
public interface PurchaseRepository {

    /** Inserts the purchase and its lines and returns them with the assigned ids. */
    PurchaseDto save(PurchaseDto purchase);

    /** Own history with its lines, from most recent to oldest; the lines in insertion order. */
    List<PurchaseDto> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** Own detail: someone else's purchase is indistinguishable from a non-existent one. */
    Optional<PurchaseDto> findByIdAndUserId(Long id, Long userId);
}
