package shop.carrito.internal.usecase;

import shop.carrito.internal.domain.Carrito;
import shop.carrito.internal.domain.CarritoRepository;
import shop.carrito.internal.domain.LineaCarrito;
import shop.carrito.internal.domain.LineaCarritoRepository;
import shop.compartido.error.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuitarDelCarrito {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;

    public QuitarDelCarrito(CarritoRepository carritos, LineaCarritoRepository lineas) {
        this.carritos = carritos;
        this.lineas = lineas;
    }

    @Transactional
    public void ejecutar(Long usuarioId, Long peliculaId) {
        Carrito carrito = carritos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> noEstaEnElCarrito(peliculaId));
        LineaCarrito linea = lineas.findByCarritoIdAndPeliculaId(carrito.getId(), peliculaId)
                .orElseThrow(() -> noEstaEnElCarrito(peliculaId));
        lineas.delete(linea);
    }

    private static RecursoNoEncontradoException noEstaEnElCarrito(Long peliculaId) {
        return new RecursoNoEncontradoException("La película " + peliculaId + " no está en el carrito");
    }
}
