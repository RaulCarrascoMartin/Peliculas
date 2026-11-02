package org.example.compra.application;

import org.example.compra.domain.Compra;
import org.example.compra.domain.LineaCompra;

import java.util.List;

/** Una compra con sus líneas congeladas, tal como la devuelve {@link CompraService}. */
public record CompraVista(
        Compra compra,
        List<LineaCompra> lineas) {
}
