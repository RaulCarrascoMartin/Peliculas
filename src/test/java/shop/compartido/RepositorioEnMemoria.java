package shop.compartido;

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
 * Base de los dobles de repositorio escritos a mano: guarda las entidades en un mapa
 * y asigna ids correlativos. Implementa lo básico de CRUD; el resto lanza
 * {@link UnsupportedOperationException} y cada doble añade las consultas que necesite.
 */
public abstract class RepositorioEnMemoria<T> implements JpaRepository<T, Long> {

    protected final Map<Long, T> entidades = new LinkedHashMap<>();
    private long siguienteId = 1;

    protected abstract Long idDe(T entidad);

    protected abstract void asignarId(T entidad, Long id);

    @Override
    public <S extends T> S save(S entidad) {
        if (idDe(entidad) == null) {
            asignarId(entidad, siguienteId++);
        }
        entidades.put(idDe(entidad), entidad);
        return entidad;
    }

    @Override
    public <S extends T> S saveAndFlush(S entidad) {
        return save(entidad);
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> lote) {
        List<S> guardadas = new ArrayList<>();
        lote.forEach(entidad -> guardadas.add(save(entidad)));
        return guardadas;
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(entidades.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return entidades.containsKey(id);
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(entidades.values());
    }

    @Override
    public long count() {
        return entidades.size();
    }

    @Override
    public void deleteById(Long id) {
        entidades.remove(id);
    }

    @Override
    public void delete(T entidad) {
        entidades.remove(idDe(entidad));
    }

    @Override
    public void deleteAll() {
        entidades.clear();
    }

    @Override
    public void flush() {
    }

    @Override
    public List<T> findAllById(Iterable<Long> ids) {
        throw sinSoporte();
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        throw sinSoporte();
    }

    @Override
    public void deleteAll(Iterable<? extends T> lote) {
        throw sinSoporte();
    }

    @Override
    public List<T> findAll(Sort orden) {
        throw sinSoporte();
    }

    @Override
    public Page<T> findAll(Pageable paginacion) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> List<S> saveAllAndFlush(Iterable<S> lote) {
        throw sinSoporte();
    }

    @Override
    public void deleteAllInBatch(Iterable<T> lote) {
        throw sinSoporte();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        throw sinSoporte();
    }

    @Override
    public void deleteAllInBatch() {
        throw sinSoporte();
    }

    @Override
    public T getOne(Long id) {
        throw sinSoporte();
    }

    @Override
    public T getById(Long id) {
        throw sinSoporte();
    }

    @Override
    public T getReferenceById(Long id) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> ejemplo) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> ejemplo, Sort orden) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> Optional<S> findOne(Example<S> ejemplo) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> ejemplo, Pageable paginacion) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> long count(Example<S> ejemplo) {
        throw sinSoporte();
    }

    @Override
    public <S extends T> boolean exists(Example<S> ejemplo) {
        throw sinSoporte();
    }

    @Override
    public <S extends T, R> R findBy(Example<S> ejemplo,
                                     Function<FluentQuery.FetchableFluentQuery<S>, R> consulta) {
        throw sinSoporte();
    }

    private static UnsupportedOperationException sinSoporte() {
        return new UnsupportedOperationException("No implementado en el doble de prueba");
    }
}
