package org.example.carrito.application;

import java.math.BigDecimal;
import java.util.List;

/** Estado actual del carrito de un usuario: líneas con precio vivo y total. */
public record CarritoVista(
        List<LineaDeCarrito> lineas,
        BigDecimal total) {
}
