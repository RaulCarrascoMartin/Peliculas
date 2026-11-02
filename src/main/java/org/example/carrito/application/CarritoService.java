package org.example.carrito.application;

import org.example.carrito.CarritoApi;
import org.example.carrito.domain.Carrito;
import org.example.carrito.domain.CarritoRepository;
import org.example.carrito.domain.LineaCarrito;
import org.example.carrito.domain.LineaCarritoRepository;
import org.example.compartido.ConflictoException;
import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.PeliculaApi;
import org.example.pelicula.PeliculaResumen;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestiona el carrito propio y expone lectura/vaciado al módulo compra vía {@link CarritoApi}.
 * El precio nunca se persiste en la línea: se calcula al leer con el precio vivo de {@code pelicula}.
 */
@Service
public class CarritoService implements CarritoApi {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;
    private final PeliculaApi peliculas;

    public CarritoService(CarritoRepository carritos, LineaCarritoRepository lineas, PeliculaApi peliculas) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.peliculas = peliculas;
    }

    public CarritoVista ver(Long usuarioId) {
        return carritos.findByUsuarioId(usuarioId)
                .map(this::vista)
                .orElseGet(() -> new CarritoVista(List.of(), BigDecimal.ZERO));
    }

    private CarritoVista vista(Carrito carrito) {
        List<LineaCarrito> guardadas = lineas.findByCarritoIdOrderById(carrito.getId());
        Map<Long, PeliculaResumen> resumenPorId = peliculas.resumenes(ids(guardadas)).stream()
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

    private static List<Long> ids(List<LineaCarrito> lineasGuardadas) {
        return lineasGuardadas.stream().map(LineaCarrito::getPeliculaId).toList();
    }

    @Transactional
    public LineaDeCarrito anadir(Long usuarioId, Long peliculaId) {
        List<PeliculaResumen> resumenes = peliculas.resumenes(List.of(peliculaId));
        if (resumenes.isEmpty()) {
            throw new RecursoNoEncontradoException("La película " + peliculaId + " no existe");
        }
        Carrito carrito = obtenerOCrear(usuarioId);
        if (lineas.findByCarritoIdAndPeliculaId(carrito.getId(), peliculaId).isPresent()) {
            throw new ConflictoException("La película ya está en el carrito");
        }
        PeliculaResumen resumen = resumenes.get(0);
        if (resumen.precio() == null) {
            throw new ConflictoException("La película no tiene precio y no es comprable");
        }
        LineaCarrito linea = new LineaCarrito();
        linea.setCarritoId(carrito.getId());
        linea.setPeliculaId(peliculaId);
        lineas.save(linea);
        return new LineaDeCarrito(peliculaId, resumen.titulo(), resumen.precio());
    }

    /** La primera línea crea el carrito; en la carrera del UNIQUE lo gana otro hilo y se relee su carrito. */
    private Carrito obtenerOCrear(Long usuarioId) {
        return carritos.findByUsuarioId(usuarioId).orElseGet(() -> crearCarrito(usuarioId));
    }

    private Carrito crearCarrito(Long usuarioId) {
        var carrito = new Carrito();
        carrito.setUsuarioId(usuarioId);
        try {
            return carritos.save(carrito);
        } catch (DataIntegrityViolationException e) {
            return carritos.findByUsuarioId(usuarioId).orElseThrow();
        }
    }

    @Transactional
    public void quitar(Long usuarioId, Long peliculaId) {
        Carrito carrito = carritos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La película " + peliculaId + " no está en el carrito"));
        LineaCarrito linea = lineas.findByCarritoIdAndPeliculaId(carrito.getId(), peliculaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La película " + peliculaId + " no está en el carrito"));
        lineas.delete(linea);
    }

    @Override
    @Transactional
    public void vaciar(Long usuarioId) {
        carritos.findByUsuarioId(usuarioId).ifPresent(carrito -> {
            lineas.deleteByCarritoId(carrito.getId());
            // Cambio real de la entidad: fuerza el update con @Version, que hace perder al segundo checkout simultáneo.
            carrito.setActualizadoEn(LocalDateTime.now());
            carritos.save(carrito);
        });
    }

    @Override
    public List<Long> peliculasEnCarrito(Long usuarioId) {
        return carritos.findByUsuarioId(usuarioId)
                .map(carrito -> ids(lineas.findByCarritoIdOrderById(carrito.getId())))
                .orElse(List.of());
    }
}
