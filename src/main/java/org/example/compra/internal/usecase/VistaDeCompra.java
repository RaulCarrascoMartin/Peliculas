package org.example.compra.internal.usecase;

import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.LineaCompra;

import java.util.List;

/** Convierte las entidades en los records que salen de los casos de uso: el modelo nunca cruza este límite. */
final class VistaDeCompra {

    private VistaDeCompra() {
    }

    static CompraVista de(Compra compra, List<LineaCompra> lineas) {
        return new CompraVista(compra.getId(), compra.getUsuarioId(), compra.getFecha(), compra.getTotal(),
                compra.getEstado().name(),
                lineas.stream()
                        .map(l -> new LineaCompraVista(l.getPeliculaId(), l.getTitulo(), l.getPrecioUnitario()))
                        .toList());
    }
}
