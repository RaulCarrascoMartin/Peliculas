package org.example.carrito.internal.usecase;

import org.example.carrito.internal.domain.Carrito;
import org.example.carrito.internal.domain.CarritoRepository;
import org.example.carrito.internal.domain.LineaCarrito;
import org.example.carrito.internal.domain.LineaCarritoRepository;
import org.example.compartido.error.ConflictoException;
import org.example.compartido.error.RecursoNoEncontradoException;
import org.example.pelicula.api.PeliculaApi;
import org.example.pelicula.api.PeliculaResumen;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnadirAlCarrito {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;
    private final PeliculaApi peliculas;

    public AnadirAlCarrito(CarritoRepository carritos, LineaCarritoRepository lineas, PeliculaApi peliculas) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.peliculas = peliculas;
    }

    @Transactional
    public LineaDeCarrito ejecutar(Long usuarioId, Long peliculaId) {
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
}
