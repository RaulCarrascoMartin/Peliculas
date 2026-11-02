package org.example.compra.application;

import org.example.carrito.CarritoApi;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble de prueba de {@link CarritoApi}: carritos como listas de ids de película por usuario. */
public class CarritoApiFalsa implements CarritoApi {

    private final Map<Long, List<Long>> carritos = new HashMap<>();
    private boolean fallarAlVaciarPorBloqueoOptimista;

    public void anadir(Long usuarioId, Long peliculaId) {
        carritos.computeIfAbsent(usuarioId, id -> new ArrayList<>()).add(peliculaId);
    }

    /** Simula que el segundo checkout simultáneo pierde la carrera del {@code @Version} al vaciar. */
    public void fallarAlVaciarPorBloqueoOptimista() {
        fallarAlVaciarPorBloqueoOptimista = true;
    }

    @Override
    public List<Long> peliculasEnCarrito(Long usuarioId) {
        return List.copyOf(carritos.getOrDefault(usuarioId, List.of()));
    }

    @Override
    public void vaciar(Long usuarioId) {
        if (fallarAlVaciarPorBloqueoOptimista) {
            throw new ObjectOptimisticLockingFailureException("Carrito", usuarioId);
        }
        carritos.remove(usuarioId);
    }
}
