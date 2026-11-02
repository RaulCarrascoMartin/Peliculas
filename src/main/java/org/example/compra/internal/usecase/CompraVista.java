package org.example.compra.internal.usecase;

import org.example.compra.internal.domain.Compra;
import org.example.compra.internal.domain.LineaCompra;

import java.util.List;

/** Una compra con sus líneas congeladas, tal como la devuelve los casos de uso de compra. */
public record CompraVista(
        Compra compra,
        List<LineaCompra> lineas) {
}
