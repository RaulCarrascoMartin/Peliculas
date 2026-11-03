package shop.purchase.internal.persistence.repository;

import shop.purchase.internal.persistence.jpa.PurchaseLineJpaRepository;
import shop.purchase.internal.persistence.mapper.PurchasePersistenceMapper;

import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;

import java.util.List;

public class PurchaseLineRepositoryImpl implements PurchaseLineRepository {

    private final PurchaseLineJpaRepository dao;

    public PurchaseLineRepositoryImpl(PurchaseLineJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public PurchaseLine save(PurchaseLine line) {
        return PurchasePersistenceMapper.toDomain(dao.save(PurchasePersistenceMapper.toEntity(line)));
    }

    @Override
    public List<PurchaseLine> findByPurchaseIdOrderById(Long purchaseId) {
        return dao.findByPurchaseIdOrderById(purchaseId).stream().map(PurchasePersistenceMapper::toDomain).toList();
    }
}
