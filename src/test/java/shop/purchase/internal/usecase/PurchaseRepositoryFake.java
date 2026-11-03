package shop.purchase.internal.usecase;

import shop.shared.InMemoryRepository;
import shop.purchase.internal.domain.Purchase;
import shop.purchase.internal.domain.PurchaseRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PurchaseRepositoryFake extends InMemoryRepository<Purchase> implements PurchaseRepository {

    @Override
    protected Long idOf(Purchase purchase) {
        return purchase.getId();
    }

    @Override
    protected void assignId(Purchase purchase, Long id) {
        purchase.setId(id);
    }

    @Override
    public List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId) {
        return entities.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .sorted(Comparator.comparing(Purchase::getPurchasedAt).reversed())
                .toList();
    }

    @Override
    public Optional<Purchase> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(entities.get(id)).filter(c -> c.getUserId().equals(userId));
    }
}
