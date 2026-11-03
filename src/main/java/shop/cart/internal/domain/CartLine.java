package shop.cart.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** What a user wants to buy: one line per movie, no quantities; the price is never persisted here. */
@Entity
@Table(name = "lineas_carrito",
        uniqueConstraints = @UniqueConstraint(columnNames = {"carrito_id", "pelicula_id"}))
public class CartLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrito_id", nullable = false)
    private Long cartId;

    @Column(name = "pelicula_id", nullable = false)
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
