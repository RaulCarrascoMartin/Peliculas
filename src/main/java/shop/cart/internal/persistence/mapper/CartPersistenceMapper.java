package shop.cart.internal.persistence.mapper;

import shop.cart.internal.persistence.entity.CartEntity;
import shop.cart.internal.persistence.entity.CartLineEntity;

import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;

/** Converts between the framework-free domain model and the JPA entities. */
public final class CartPersistenceMapper {

    private CartPersistenceMapper() {
    }

    public static Cart toDomain(CartEntity e) {
        var d = new Cart();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setVersion(e.getVersion());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public static CartEntity toEntity(Cart d) {
        var e = new CartEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setVersion(d.getVersion());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public static CartLine toDomain(CartLineEntity e) {
        var d = new CartLine();
        d.setId(e.getId());
        d.setCartId(e.getCartId());
        d.setMovieId(e.getMovieId());
        return d;
    }

    public static CartLineEntity toEntity(CartLine d) {
        var e = new CartLineEntity();
        e.setId(d.getId());
        e.setCartId(d.getCartId());
        e.setMovieId(d.getMovieId());
        return e;
    }
}
