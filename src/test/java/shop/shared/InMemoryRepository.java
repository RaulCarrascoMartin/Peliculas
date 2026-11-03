package shop.shared;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base of the hand-written repository doubles: stores the entities in a map
 * and assigns sequential ids. Implements basic CRUD; the rest throws
 * {@link UnsupportedOperationException} and each double adds the queries it needs.
 */
public abstract class InMemoryRepository<T> implements JpaRepository<T, Long> {

    protected final Map<Long, T> entities = new LinkedHashMap<>();
    private long nextId = 1;

    protected abstract Long idOf(T entity);

    protected abstract void assignId(T entity, Long id);

    @Override
    public <S extends T> S save(S entity) {
        if (idOf(entity) == null) {
            assignId(entity, nextId++);
        }
        entities.put(idOf(entity), entity);
        return entity;
    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> lote) {
        List<S> storedLines = new ArrayList<>();
        lote.forEach(entity -> storedLines.add(save(entity)));
        return storedLines;
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return entities.containsKey(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void deleteById(Long id) {
        entities.remove(id);
    }

    @Override
    public void delete(T entity) {
        entities.remove(idOf(entity));
    }

    @Override
    public void deleteAll() {
        entities.clear();
    }

    @Override
    public void flush() {
    }

    @Override
    public List<T> findAllById(Iterable<Long> ids) {
        throw unsupported();
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        throw unsupported();
    }

    @Override
    public void deleteAll(Iterable<? extends T> lote) {
        throw unsupported();
    }

    @Override
    public List<T> findAll(Sort order) {
        throw unsupported();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        throw unsupported();
    }

    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> lote) {
        throw unsupported();
    }

    @Override
    public void deleteAllInBatch(Iterable<T> lote) {
        throw unsupported();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw unsupported();
    }

    @Override
    public void deleteAllInBatch() {
        throw unsupported();
    }

    @Override
    public T getOne(Long id) {
        throw unsupported();
    }

    @Override
    public T getById(Long id) {
        throw unsupported();
    }

    @Override
    public T getReferenceById(Long id) {
        throw unsupported();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        throw unsupported();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort order) {
        throw unsupported();
    }

    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        throw unsupported();
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw unsupported();
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        throw unsupported();
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        throw unsupported();
    }

    @Override
    public <S extends T, R> R findBy(Example<S> example,
                                     Function<FluentQuery.FetchableFluentQuery<S>, R> query) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Not implemented in the test double");
    }
}
