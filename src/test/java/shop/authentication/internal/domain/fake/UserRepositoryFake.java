package shop.authentication.internal.domain.fake;

import shop.authentication.internal.domain.model.User;
import shop.authentication.internal.domain.repository.UserRepository;
import shop.shared.InMemoryRepository;

import java.util.Optional;

public class UserRepositoryFake extends InMemoryRepository<User> implements UserRepository {

    @Override
    protected Long idOf(User user) {
        return user.getId();
    }

    @Override
    protected void assignId(User user, Long id) {
        user.setId(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return entities.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
