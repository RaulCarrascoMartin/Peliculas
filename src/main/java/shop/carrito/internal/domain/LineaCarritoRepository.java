package shop.carrito.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineaCarritoRepository extends JpaRepository<LineaCarrito, Long> {

    List<LineaCarrito> findByCarritoIdOrderById(Long carritoId);

    /** Una línea por película y carrito (UNIQUE en carritoId + peliculaId). */
    Optional<LineaCarrito> findByCarritoIdAndPeliculaId(Long carritoId, Long peliculaId);

    boolean existsByCarritoIdAndPeliculaId(Long carritoId, Long peliculaId);

    /** Limpieza al eliminar una película del catálogo: quita su línea de todos los carritos. */
    void deleteByPeliculaId(Long peliculaId);

    /** Vaciado del carrito: quita todas sus líneas. */
    void deleteByCarritoId(Long carritoId);
}
