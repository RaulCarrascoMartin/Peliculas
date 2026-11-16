package shop.cart.internal.domain.model;

/** What a user wants to buy: one line per movie, no quantities; the price is never persisted here. */
public class CartLine {

    private Long id;

    private Long cartId;

    private Long movieId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }
}
