package shop.purchase.internal.persistence.mapper;

import shop.purchase.internal.persistence.entity.PurchaseEntity;
import shop.purchase.internal.persistence.entity.PurchaseLineEntity;

import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.model.PurchaseStatus;

/** Converts between the framework-free domain model and the JPA entities. */
public final class PurchasePersistenceMapper {

    private PurchasePersistenceMapper() {
    }

    public static Purchase toDomain(PurchaseEntity e) {
        var d = new Purchase();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setPurchasedAt(e.getPurchasedAt());
        d.setTotal(e.getTotal());
        d.setStatus(e.getStatus());
        return d;
    }

    public static PurchaseEntity toEntity(Purchase d) {
        var e = new PurchaseEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setPurchasedAt(d.getPurchasedAt());
        e.setTotal(d.getTotal());
        e.setStatus(d.getStatus());
        return e;
    }

    public static PurchaseLine toDomain(PurchaseLineEntity e) {
        var d = new PurchaseLine();
        d.setId(e.getId());
        d.setPurchaseId(e.getPurchaseId());
        d.setMovieId(e.getMovieId());
        d.setTitle(e.getTitle());
        d.setUnitPrice(e.getUnitPrice());
        return d;
    }

    public static PurchaseLineEntity toEntity(PurchaseLine d) {
        var e = new PurchaseLineEntity();
        e.setId(d.getId());
        e.setPurchaseId(d.getPurchaseId());
        e.setMovieId(d.getMovieId());
        e.setTitle(d.getTitle());
        e.setUnitPrice(d.getUnitPrice());
        return e;
    }
}
