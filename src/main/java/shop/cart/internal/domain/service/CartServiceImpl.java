package shop.cart.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.cart.internal.domain.contract.MovieCatalog;
import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.dto.CatalogMovieDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.exception.MovieAlreadyInCartException;
import shop.cart.internal.domain.exception.MovieNotFoundException;
import shop.cart.internal.domain.exception.MovieNotInCartException;
import shop.cart.internal.domain.exception.MovieWithoutPriceException;
import shop.cart.internal.domain.mapper.CartMapper;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The writes are transactional with the default propagation: called on its own, each one is a unit of work; called
 * by {@code purchase} through the facade, {@link #clear} joins the transaction of the checkout, so the cart is only
 * emptied if the purchase commits and its version check makes the second simultaneous checkout lose.
 */
@Service
public class CartServiceImpl implements CartService {

    private final CartRepository carts;
    private final MovieCatalog movies;
    private final CartMapper mapper;

    public CartServiceImpl(CartRepository carts, MovieCatalog movies, CartMapper mapper) {
        this.carts = carts;
        this.movies = movies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PricedLineDto addMovie(Long userId, Long movieId) {
        CatalogMovieDto movie = purchasableMovie(movieId);
        Cart cart = getOrCreate(userId);
        if (carts.existsLine(cart.getId(), movieId)) {
            throw new MovieAlreadyInCartException(movieId);
        }
        carts.addLine(mapper.toDto(newLine(cart, movieId)));
        return mapper.toPricedLine(movie);
    }

    @Override
    @Transactional
    public void removeMovie(Long userId, Long movieId) {
        Cart cart = find(userId).orElseThrow(() -> new MovieNotInCartException(movieId));
        if (!carts.removeLine(cart.getId(), movieId)) {
            throw new MovieNotInCartException(movieId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CartContentsDto contentsOf(Long userId) {
        return find(userId)
                .map(this::contentsOf)
                .orElseGet(() -> new CartContentsDto(List.of(), BigDecimal.ZERO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> movieIdsOf(Long userId) {
        return find(userId)
                .map(cart -> carts.findLines(cart.getId()).stream().map(CartLineDto::movieId).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        find(userId).ifPresent(cart -> {
            carts.removeLines(cart.getId());
            // Real change of the cart: forces the update with the version, which makes the second simultaneous
            // checkout lose.
            cart.setUpdatedAt(LocalDateTime.now());
            carts.save(mapper.toDto(cart));
        });
    }

    private Optional<Cart> find(Long userId) {
        return carts.findByUserId(userId).map(mapper::toModel);
    }

    private CartContentsDto contentsOf(Cart cart) {
        List<Long> stored = carts.findLines(cart.getId()).stream().map(CartLineDto::movieId).toList();
        Map<Long, CatalogMovieDto> byId = movies.findByIds(stored).stream()
                .collect(Collectors.toMap(CatalogMovieDto::id, Function.identity()));
        // Defence: a line whose movie no longer exists is skipped (the catalog ignores unknown ids).
        List<PricedLineDto> priced = stored.stream()
                .filter(byId::containsKey)
                .map(movieId -> mapper.toPricedLine(byId.get(movieId)))
                .toList();
        BigDecimal total = priced.stream().map(PricedLineDto::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartContentsDto(priced, total);
    }

    /** The movie must exist in the catalog and have a price. */
    private CatalogMovieDto purchasableMovie(Long movieId) {
        CatalogMovieDto movie = movies.findByIds(List.of(movieId)).stream()
                .findFirst()
                .orElseThrow(() -> new MovieNotFoundException(movieId));
        if (movie.price() == null) {
            throw new MovieWithoutPriceException(movieId);
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
        return find(userId).orElseGet(() -> createCart(userId));
    }

    private Cart createCart(Long userId) {
        var cart = new Cart();
        cart.setUserId(userId);
        try {
            return mapper.toModel(carts.save(mapper.toDto(cart)));
        } catch (CartAlreadyExistsException e) {
            return find(userId).orElseThrow();
        }
    }
}
