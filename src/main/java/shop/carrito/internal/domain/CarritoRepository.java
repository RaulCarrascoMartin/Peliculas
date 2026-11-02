package shop.carrito.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    /** El carrito de un usuario existe a lo sumo una vez (UNIQUE en usuarioId). */
    Optional<Carrito> findByUsuarioId(Long usuarioId);
}
