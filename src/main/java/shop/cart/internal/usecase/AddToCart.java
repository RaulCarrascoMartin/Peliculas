package shop.cart.internal.usecase;

import shop.cart.internal.domain.Cart;
import shop.cart.internal.domain.CartRepository;
import shop.cart.internal.domain.CartLine;
import shop.cart.internal.domain.CartLineRepository;
import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.MovieSummary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddToCart {

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final CatalogApi movies;

    public AddToCart(CartRepository carts, CartLineRepository lines, CatalogApi movies) {
        this.carts = carts;
        this.lines = lines;
        this.movies = movies;
    }

    @Transactional
    public CartLineView execute(Long userId, Long movieId) {
        MovieSummary movie = purchasableMovie(movieId);
        Cart cart = getOrCreate(userId);
        if (lines.existsByCartIdAndMovieId(cart.getId(), movieId)) {
            throw new ConflictException("La película ya está en el carrito");
        }
        lines.save(newLine(cart, movieId));
        return new CartLineView(movieId, movie.title(), movie.price());
    }

    /** The movie must exist in the catalog and have a price. */
    private MovieSummary purchasableMovie(Long movieId) {

        MovieSummary movie = movies.findSummaries(List.of(movieId)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("La película " + movieId + " no existe"));

        if (movie.price() == null) {
            throw new ConflictException("La película no tiene precio y no es comprable");
        }

        return movie;
    }

    private static CartLine newLine(Cart cart, Long movieId) {
        var line = new CartLine();
        line.setCartId(cart.getId());
        line.setMovieId(movieId);
        return line;
    }

    /** The first line creates the cart; in the UNIQUE race another thread wins and its cart is re-read. */
    private Cart getOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        var cart = new Cart();
        cart.setUserId(userId);
        try {
            return carts.save(cart);
        } catch (DataIntegrityViolationException e) {
            return carts.findByUserId(userId).orElseThrow();
        }
    }
}
