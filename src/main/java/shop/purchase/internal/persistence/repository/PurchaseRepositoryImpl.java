package shop.purchase.internal.persistence.repository;

import shop.purchase.internal.persistence.jpa.PurchaseJpaRepository;
import shop.purchase.internal.persistence.mapper.PurchasePersistenceMapper;

import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.util.List;
import java.util.Optional;

public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final PurchaseJpaRepository dao;

    public PurchaseRepositoryImpl(PurchaseJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public Purchase save(Purchase purchase) {
        return PurchasePersistenceMapper.toDomain(dao.save(PurchasePersistenceMapper.toEntity(purchase)));
    }

    @Override
    public List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId) {
        return dao.findByUserIdOrderByPurchasedAtDesc(userId).stream().map(PurchasePersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Purchase> findByIdAndUserId(Long id, Long userId) {
        return dao.findByIdAndUserId(id, userId).map(PurchasePersistenceMapper::toDomain);
    }
}
