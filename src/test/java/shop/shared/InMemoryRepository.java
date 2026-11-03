package shop.shared;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base of the hand-written repository doubles: stores the models in a map and assigns sequential ids.
 * Implements basic CRUD; each double adds the queries of its domain repository.
 */
public abstract class InMemoryRepository<T> {

    protected final Map<Long, T> entities = new LinkedHashMap<>();
    private long nextId = 1;

    protected abstract Long idOf(T entity);

    protected abstract void assignId(T entity, Long id);

    public T save(T entity) {
        if (idOf(entity) == null) {
            assignId(entity, nextId++);
        }
        entities.put(idOf(entity), entity);
        return entity;
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entities.get(id));
    }

    public boolean existsById(Long id) {
        return entities.containsKey(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    public long count() {
        return entities.size();
    }

    public void deleteById(Long id) {
        entities.remove(id);
    }

    public void delete(T entity) {
        entities.remove(idOf(entity));
    }

    public void deleteAll() {
        entities.clear();
    }
}
