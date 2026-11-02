package org.example.carrito.internal.usecase;

import org.example.carrito.internal.domain.Carrito;
import org.example.carrito.internal.domain.CarritoRepository;
import org.example.compartido.RepositorioEnMemoria;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class CarritoRepositoryFalso extends RepositorioEnMemoria<Carrito> implements CarritoRepository {

    /** usuarioId cuyo carrito la próxima consulta devuelve vacío: simula la lectura en plena carrera. */
    private Long ocultarEnProximaConsulta;

    public void ocultarElCarritoEnProximaConsulta(Long usuarioId) {
        ocultarEnProximaConsulta = usuarioId;
    }

    @Override
    protected Long idDe(Carrito carrito) {
        return carrito.getId();
    }

    @Override
    protected void asignarId(Carrito carrito, Long id) {
        carrito.setId(id);
    }

    @Override
    public <S extends Carrito> S save(S carrito) {
        // Simula el UNIQUE(usuarioId) que el doble real aplica al persistir un carrito nuevo.
        if (idDe(carrito) == null && entidades.values().stream()
                .anyMatch(c -> c.getUsuarioId().equals(carrito.getUsuarioId()))) {
            throw new DataIntegrityViolationException("Carrito repetido para el mismo usuario");
        }
        return super.save(carrito);
    }

    @Override
    public Optional<Carrito> findByUsuarioId(Long usuarioId) {
        Optional<Carrito> encontrada = entidades.values().stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .findFirst();
        if (usuarioId.equals(ocultarEnProximaConsulta)) {
            ocultarEnProximaConsulta = null;
            return Optional.empty();
        }
        return encontrada;
    }
}
