package shop.cart.internal.persistence.repository;

import shop.cart.internal.persistence.jpa.CartLineJpaRepository;
import shop.cart.internal.persistence.mapper.CartPersistenceMapper;

import shop.cart.internal.domain.model.CartLine;
import shop.cart.internal.domain.repository.CartLineRepository;

import java.util.List;
import java.util.Optional;

public class CartLineRepositoryImpl implements CartLineRepository {

    private final CartLineJpaRepository dao;

    public CartLineRepositoryImpl(CartLineJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public CartLine save(CartLine line) {
        return CartPersistenceMapper.toDomain(dao.save(CartPersistenceMapper.toEntity(line)));
    }

    @Override
    public void delete(CartLine line) {
        dao.deleteById(line.getId());
    }

    @Override
    public List<CartLine> findByCartIdOrderById(Long cartId) {
        return dao.findByCartIdOrderById(cartId).stream().map(CartPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<CartLine> findByCartIdAndMovieId(Long cartId, Long movieId) {
        return dao.findByCartIdAndMovieId(cartId, movieId).map(CartPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCartIdAndMovieId(Long cartId, Long movieId) {
        return dao.existsByCartIdAndMovieId(cartId, movieId);
    }

    @Override
    public void deleteByMovieId(Long movieId) {
        dao.deleteByMovieId(movieId);
    }

    @Override
    public void deleteByCartId(Long cartId) {
        dao.deleteByCartId(cartId);
    }
}
