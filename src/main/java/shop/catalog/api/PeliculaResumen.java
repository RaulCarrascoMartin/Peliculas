package shop.catalog.api;

import java.math.BigDecimal;

/** Datos públicos de una película para otros módulos; evita exponer la entidad. */
public record PeliculaResumen(
        Long id,
        String titulo,
        Integer anio,
        String genero,
        String director,
        BigDecimal precio,
        Double valoracionMedia) {
}
