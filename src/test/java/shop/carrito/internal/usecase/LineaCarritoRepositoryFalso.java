package shop.carrito.internal.usecase;

import shop.carrito.internal.domain.LineaCarrito;
import shop.carrito.internal.domain.LineaCarritoRepository;
import shop.compartido.RepositorioEnMemoria;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LineaCarritoRepositoryFalso extends RepositorioEnMemoria<LineaCarrito>
        implements LineaCarritoRepository {

    @Override
    public <S extends LineaCarrito> S save(S entidad) {
        if (entidad.getId() == null) {
            // Simula el UNIQUE (carritoId, peliculaId) que el doble real aplica al persistir.
            boolean repetida = entidades.values().stream().anyMatch(l ->
                    l.getCarritoId().equals(entidad.getCarritoId())
                            && l.getPeliculaId().equals(entidad.getPeliculaId()));
            if (repetida) {
                throw new DataIntegrityViolationException("Línea repetida");
            }
        }
        return super.save(entidad);
    }

    @Override
    protected Long idDe(LineaCarrito linea) {
        return linea.getId();
    }

    @Override
    protected void asignarId(LineaCarrito linea, Long id) {
        linea.setId(id);
    }

    @Override
    public List<LineaCarrito> findByCarritoIdOrderById(Long carritoId) {
        return entidades.values().stream()
                .filter(l -> l.getCarritoId().equals(carritoId))
                .sorted(Comparator.comparingLong(l -> l.getId() == null ? Long.MAX_VALUE : l.getId()))
                .toList();
    }

    @Override
    public Optional<LineaCarrito> findByCarritoIdAndPeliculaId(Long carritoId, Long peliculaId) {
        return entidades.values().stream()
                .filter(l -> l.getCarritoId().equals(carritoId) && l.getPeliculaId().equals(peliculaId))
                .findFirst();
    }

    @Override
    public void deleteByPeliculaId(Long peliculaId) {
        entidades.values().removeIf(l -> l.getPeliculaId().equals(peliculaId));
    }

    @Override
    public void deleteByCarritoId(Long carritoId) {
        entidades.values().removeIf(l -> l.getCarritoId().equals(carritoId));
    }
}
