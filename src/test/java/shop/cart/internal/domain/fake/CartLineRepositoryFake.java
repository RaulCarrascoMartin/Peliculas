package shop.cart.internal.domain.fake;

import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;
import shop.shared.InMemoryRepository;


import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CartLineRepositoryFake extends InMemoryRepository<CartLine>
        implements CartLineRepository {

    @Override
    public CartLine save(CartLine entity) {
        if (entity.getId() == null) {
            // Simulates the UNIQUE (cart_id, movie_id) that the real double enforces on persist.
            boolean repeated = entities.values().stream().anyMatch(l ->
                    l.getCartId().equals(entity.getCartId())
                            && l.getMovieId().equals(entity.getMovieId()));
            if (repeated) {
                throw new IllegalStateException("Repeated line");
            }
        }
        return super.save(entity);
    }

    @Override
    protected Long idOf(CartLine line) {
        return line.getId();
    }

    @Override
    protected void assignId(CartLine line, Long id) {
        line.setId(id);
    }

    @Override
    public List<CartLine> findByCartIdOrderById(Long cartId) {
        return entities.values().stream()
                .filter(l -> l.getCartId().equals(cartId))
                .sorted(Comparator.comparingLong(l -> l.getId() == null ? Long.MAX_VALUE : l.getId()))
                .toList();
    }

    @Override
    public Optional<CartLine> findByCartIdAndMovieId(Long cartId, Long movieId) {
        return entities.values().stream()
                .filter(l -> l.getCartId().equals(cartId) && l.getMovieId().equals(movieId))
                .findFirst();
    }

    @Override
    public boolean existsByCartIdAndMovieId(Long cartId, Long movieId) {
        return findByCartIdAndMovieId(cartId, movieId).isPresent();
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        entities.values().removeIf(l -> l.getMovieId().equals(movieId));
    }

    @Override
    public void deleteByCartId(Long cartId) {
        entities.values().removeIf(l -> l.getCartId().equals(cartId));
    }
}
