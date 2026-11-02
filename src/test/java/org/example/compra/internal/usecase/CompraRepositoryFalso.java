package org.example.compra.internal.usecase;

import org.example.compartido.RepositorioEnMemoria;
import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.CompraRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CompraRepositoryFalso extends RepositorioEnMemoria<Compra> implements CompraRepository {

    @Override
    protected Long idDe(Compra compra) {
        return compra.getId();
    }

    @Override
    protected void asignarId(Compra compra, Long id) {
        compra.setId(id);
    }

    @Override
    public List<Compra> findByUsuarioIdOrderByFechaDesc(Long usuarioId) {
        return entidades.values().stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .sorted(Comparator.comparing(Compra::getFecha).reversed())
                .toList();
    }

    @Override
    public Optional<Compra> findByIdAndUsuarioId(Long id, Long usuarioId) {
        return Optional.ofNullable(entidades.get(id)).filter(c -> c.getUsuarioId().equals(usuarioId));
    }
}
