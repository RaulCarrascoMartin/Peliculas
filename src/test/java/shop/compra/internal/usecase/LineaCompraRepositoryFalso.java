package shop.compra.internal.usecase;

import shop.compartido.RepositorioEnMemoria;
import shop.compra.internal.domain.LineaCompra;
import shop.compra.internal.domain.LineaCompraRepository;

import java.util.Comparator;
import java.util.List;

public class LineaCompraRepositoryFalso extends RepositorioEnMemoria<LineaCompra>
        implements LineaCompraRepository {

    @Override
    protected Long idDe(LineaCompra linea) {
        return linea.getId();
    }

    @Override
    protected void asignarId(LineaCompra linea, Long id) {
        linea.setId(id);
    }

    @Override
    public List<LineaCompra> findByCompraIdOrderById(Long compraId) {
        return entidades.values().stream()
                .filter(l -> l.getCompraId().equals(compraId))
                .sorted(Comparator.comparingLong(l -> l.getId() == null ? Long.MAX_VALUE : l.getId()))
                .toList();
    }
}
