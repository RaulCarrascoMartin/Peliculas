package shop.cart.internal.domain.dto;

/** Stored cart line: one per movie and cart, without quantity or price. */
public record CartLineDto(
        Long id,
        Long cartId,
        Long movieId) {
}
