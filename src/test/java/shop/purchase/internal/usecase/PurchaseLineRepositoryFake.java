package shop.purchase.internal.usecase;

import shop.shared.InMemoryRepository;
import shop.purchase.internal.domain.PurchaseLine;
import shop.purchase.internal.domain.PurchaseLineRepository;

import java.util.Comparator;
import java.util.List;

public class PurchaseLineRepositoryFake extends InMemoryRepository<PurchaseLine>
        implements PurchaseLineRepository {

    @Override
    protected Long idOf(PurchaseLine line) {
        return line.getId();
    }

    @Override
    protected void assignId(PurchaseLine line, Long id) {
        line.setId(id);
    }

    @Override
    public List<PurchaseLine> findByPurchaseIdOrderById(Long purchaseId) {
        return entities.values().stream()
                .filter(l -> l.getPurchaseId().equals(purchaseId))
                .sorted(Comparator.comparingLong(l -> l.getId() == null ? Long.MAX_VALUE : l.getId()))
                .toList();
    }
}
