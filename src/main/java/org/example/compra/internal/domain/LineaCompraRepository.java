package org.example.compra.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineaCompraRepository extends JpaRepository<LineaCompra, Long> {

    List<LineaCompra> findByCompraIdOrderById(Long compraId);
}
