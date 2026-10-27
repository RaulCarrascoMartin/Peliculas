package org.example.carrito.application;

import org.example.carrito.domain.Carrito;
import org.example.carrito.domain.CarritoRepository;
import org.example.compartido.RepositorioEnMemoria;

import java.util.Optional;

public class CarritoRepositoryFalso extends RepositorioEnMemoria<Carrito> implements CarritoRepository {

    @Override
    protected Long idDe(Carrito carrito) {
        return carrito.getId();
    }

    @Override
    protected void asignarId(Carrito carrito, Long id) {
        carrito.setId(id);
    }

    @Override
    public Optional<Carrito> findByUsuarioId(Long usuarioId) {
        return entidades.values().stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .findFirst();
    }
}
