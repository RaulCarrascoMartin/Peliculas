package shop.authentication.internal.persistence.repository;

import shop.authentication.internal.persistence.jpa.UserJpaRepository;
import shop.authentication.internal.persistence.mapper.UserPersistenceMapper;

import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;

import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository dao;

    public UserRepositoryImpl(UserJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public User save(User user) {
        return UserPersistenceMapper.toDomain(dao.save(UserPersistenceMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return dao.findByEmail(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email);
    }
}
