package shop.cart.internal.persistence.repository;

import shop.cart.internal.persistence.jpa.CartJpaRepository;
import shop.cart.internal.persistence.mapper.CartPersistenceMapper;

import org.springframework.dao.DataIntegrityViolationException;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.exception.CartAlreadyExistsException;
import shop.cart.internal.domain.repository.CartRepository;

import java.util.Optional;

public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository dao;

    public CartRepositoryImpl(CartJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public Cart save(Cart cart) {
        try {
            return CartPersistenceMapper.toDomain(dao.save(CartPersistenceMapper.toEntity(cart)));
        } catch (DataIntegrityViolationException e) {
            if (cart.getId() == null && cart.getUserId() != null) {
                throw new CartAlreadyExistsException(cart.getUserId());
            }
            throw e;
        }
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return dao.findByUserId(userId).map(CartPersistenceMapper::toDomain);
    }
}
