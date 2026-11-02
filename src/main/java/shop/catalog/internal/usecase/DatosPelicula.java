package shop.catalog.internal.usecase;

import java.math.BigDecimal;

/** Datos editables de una película: entrada de los casos de uso de alta y edición. Un stock nulo no se toca. */
public record DatosPelicula(
        String titulo,
        Integer anio,
        String genero,
        String director,
        BigDecimal precio,
        Integer stock) {
}
