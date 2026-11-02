package org.example.carrito.internal.usecase;

import org.example.carrito.internal.domain.CarritoRepository;
import org.example.carrito.internal.domain.LineaCarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VaciarCarrito {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;

    public VaciarCarrito(CarritoRepository carritos, LineaCarritoRepository lineas) {
        this.carritos = carritos;
        this.lineas = lineas;
    }

    /** Idempotente: sin carrito no hace nada. */
    @Transactional
    public void ejecutar(Long usuarioId) {
        carritos.findByUsuarioId(usuarioId).ifPresent(carrito -> {
            lineas.deleteByCarritoId(carrito.getId());
            // Cambio real de la entidad: fuerza el update con @Version, que hace perder al segundo checkout simultáneo.
            carrito.setActualizadoEn(LocalDateTime.now());
            carritos.save(carrito);
        });
    }
}
