package org.example.compra.internal.usecase;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.CompraRepository;
import org.example.compra.internal.domain.LineaCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerCompra {

    private final CompraRepository compras;
    private final LineaCompraRepository lineas;

    public ObtenerCompra(CompraRepository compras, LineaCompraRepository lineas) {
        this.compras = compras;
        this.lineas = lineas;
    }

    /** Una compra ajena es indistinguible de una inexistente: 404, nunca 403. */
    @Transactional(readOnly = true)
    public CompraVista ejecutar(Long usuarioId, Long compraId) {
        Compra compra = compras.findByIdAndUsuarioId(compraId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La compra " + compraId + " no existe"));
        return new CompraVista(compra, lineas.findByCompraIdOrderById(compra.getId()));
    }
}
