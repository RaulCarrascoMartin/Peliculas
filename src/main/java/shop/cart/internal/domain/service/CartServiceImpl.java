package shop.cart.internal.domain.service;

import shop.cart.internal.domain.contract.CatalogMovie;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;
import shop.cart.internal.domain.repository.CartRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CartServiceImpl implements CartService {

    private final CartRepository carts;
    private final CartLineRepository lines;
    private final MovieCatalog movies;

    public CartServiceImpl(CartRepository carts, CartLineRepository lines, MovieCatalog movies) {
        this.carts = carts;
        this.lines = lines;
        this.movies = movies;
    }

    @Override
    public PricedLine addMovie(Long userId, Long movieId) {
        CatalogMovie movie = purchasableMovie(movieId);
        Cart cart = getOrCreate(userId);
        if (lines.existsByCartIdAndMovieId(cart.getId(), movieId)) {
            throw new MovieAlreadyInCartException();
        }
        lines.save(newLine(cart, movieId));
        return new PricedLine(movieId, movie.title(), movie.price());
    }

    @Override
    public void removeMovie(Long userId, Long movieId) {
        Cart cart = carts.findByUserId(userId).orElseThrow(() -> new MovieNotInCartException(movieId));
        CartLine line = lines.findByCartIdAndMovieId(cart.getId(), movieId)
                .orElseThrow(() -> new MovieNotInCartException(movieId));
        lines.delete(line);
    }

    @Override
    public CartContents contentsOf(Long userId) {
        return carts.findByUserId(userId)
                .map(this::contentsOf)
                .orElseGet(() -> new CartContents(List.of(), BigDecimal.ZERO));
    }

    @Override
    public List<Long> movieIdsOf(Long userId) {
        return carts.findByUserId(userId)
                .map(cart -> lines.findByCartIdOrderById(cart.getId()).stream().map(CartLine::getMovieId).toList())
                .orElse(List.of());
    }

    @Override
    public void clear(Long userId) {
        carts.findByUserId(userId).ifPresent(cart -> {
            lines.deleteByCartId(cart.getId());
            // Real change of the cart: forces the update with the version, which makes the second simultaneous
            // checkout lose.
            cart.setUpdatedAt(LocalDateTime.now());
            carts.save(cart);
        });
    }

    private CartContents contentsOf(Cart cart) {
        List<CartLine> stored = lines.findByCartIdOrderById(cart.getId());
        Map<Long, CatalogMovie> byId = movies.findByIds(stored.stream().map(CartLine::getMovieId).toList()).stream()
                .collect(Collectors.toMap(CatalogMovie::id, Function.identity()));
        // Defence: a line whose movie no longer exists is skipped (the catalog ignores unknown ids).
        List<PricedLine> priced = stored.stream()
                .filter(line -> byId.containsKey(line.getMovieId()))
                .map(line -> {
                    CatalogMovie movie = byId.get(line.getMovieId());
                    return new PricedLine(line.getMovieId(), movie.title(), movie.price());
                })
                .toList();
        BigDecimal total = priced.stream().map(PricedLine::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartContents(priced, total);
    }

    /** The movie must exist in the catalog and have a price. */
    private CatalogMovie purchasableMovie(Long movieId) {
        CatalogMovie movie = movies.findByIds(List.of(movieId)).stream()
                .findFirst()
                .orElseThrow(() -> new MovieNotFoundException(movieId));
        if (movie.price() == null) {
            throw new MovieWithoutPriceException();
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
        } catch (CartAlreadyExistsException e) {
            return carts.findByUserId(userId).orElseThrow();
        }
    }
}
