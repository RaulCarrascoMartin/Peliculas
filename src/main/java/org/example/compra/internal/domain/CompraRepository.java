package org.example.compra.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** Historial propio, de la más reciente a la más antigua. */
    List<Compra> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /** Detalle propio: una compra ajena es indistinguible de una inexistente. */
    Optional<Compra> findByIdAndUsuarioId(Long id, Long usuarioId);
}
