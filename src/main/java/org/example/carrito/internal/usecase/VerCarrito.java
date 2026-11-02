package org.example.carrito.internal.usecase;

import org.example.carrito.internal.domain.Carrito;
import org.example.carrito.internal.domain.CarritoRepository;
import org.example.carrito.internal.domain.LineaCarrito;
import org.example.carrito.internal.domain.LineaCarritoRepository;
import org.example.pelicula.api.PeliculaApi;
import org.example.pelicula.api.PeliculaResumen;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** El precio nunca se persiste en la línea: se calcula al leer con el precio vivo de {@code pelicula}. */
@Service
public class VerCarrito {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;
    private final PeliculaApi peliculas;

    public VerCarrito(CarritoRepository carritos, LineaCarritoRepository lineas, PeliculaApi peliculas) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.peliculas = peliculas;
    }

    public CarritoVista ejecutar(Long usuarioId) {
        return carritos.findByUsuarioId(usuarioId)
                .map(this::vista)
                .orElseGet(() -> new CarritoVista(List.of(), BigDecimal.ZERO));
    }

    private CarritoVista vista(Carrito carrito) {
        List<LineaCarrito> guardadas = lineas.findByCarritoIdOrderById(carrito.getId());
        List<Long> ids = guardadas.stream().map(LineaCarrito::getPeliculaId).toList();
        Map<Long, PeliculaResumen> resumenPorId = peliculas.resumenes(ids).stream()
                .collect(Collectors.toMap(PeliculaResumen::id, Function.identity()));
        // Defensa: una línea cuya película ya no existe se omite (los resúmenes ignoran los ids inexistentes).
        List<LineaDeCarrito> vigentes = guardadas.stream()
                .filter(linea -> resumenPorId.containsKey(linea.getPeliculaId()))
                .map(linea -> {
                    PeliculaResumen resumen = resumenPorId.get(linea.getPeliculaId());
                    return new LineaDeCarrito(linea.getPeliculaId(), resumen.titulo(), resumen.precio());
                })
                .toList();
        BigDecimal total = vigentes.stream()
                .map(LineaDeCarrito::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarritoVista(vigentes, total);
    }
}
