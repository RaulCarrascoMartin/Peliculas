package org.example.carrito.internal;

import org.example.carrito.api.CarritoApi;
import org.example.carrito.internal.domain.CarritoRepository;
import org.example.carrito.internal.domain.LineaCarrito;
import org.example.carrito.internal.domain.LineaCarritoRepository;
import org.example.carrito.internal.usecase.VaciarCarrito;
import org.springframework.stereotype.Service;

import java.util.List;

/** Punto de salida del módulo: implementa {@link CarritoApi} para {@code compra}. */
@Service
public class CarritoApiImpl
        implements CarritoApi {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;
    private final VaciarCarrito vaciar;

    public CarritoApiImpl(CarritoRepository carritos, LineaCarritoRepository lineas, VaciarCarrito vaciar) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.vaciar = vaciar;
    }

    @Override
    public List<Long> peliculasEnCarrito(Long usuarioId) {
        return carritos.findByUsuarioId(usuarioId)
                .map(carrito -> lineas.findByCarritoIdOrderById(carrito.getId()).stream()
                        .map(LineaCarrito::getPeliculaId)
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void vaciar(Long usuarioId) {
        vaciar.ejecutar(usuarioId);
    }
}
