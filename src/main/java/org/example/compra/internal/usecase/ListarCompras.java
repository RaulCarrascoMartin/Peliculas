package org.example.compra.internal.usecase;

import org.example.compra.internal.domain.CompraRepository;
import org.example.compra.internal.domain.LineaCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarCompras {

    private final CompraRepository compras;
    private final LineaCompraRepository lineas;

    public ListarCompras(CompraRepository compras, LineaCompraRepository lineas) {
        this.compras = compras;
        this.lineas = lineas;
    }

    @Transactional(readOnly = true)
    public List<CompraVista> ejecutar(Long usuarioId) {
        return compras.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(compra -> new CompraVista(compra, lineas.findByCompraIdOrderById(compra.getId())))
                .toList();
    }
}
