package org.example.compra.application;

import org.example.compartido.RepositorioEnMemoria;
import org.example.compra.domain.LineaCompra;
import org.example.compra.domain.LineaCompraRepository;

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
