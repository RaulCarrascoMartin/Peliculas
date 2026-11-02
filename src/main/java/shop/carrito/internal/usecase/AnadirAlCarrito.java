package shop.carrito.internal.usecase;

import shop.carrito.internal.domain.Carrito;
import shop.carrito.internal.domain.CarritoRepository;
import shop.carrito.internal.domain.LineaCarrito;
import shop.carrito.internal.domain.LineaCarritoRepository;
import shop.compartido.error.ConflictoException;
import shop.compartido.error.RecursoNoEncontradoException;
import shop.catalog.api.CatalogApi;
import shop.catalog.api.PeliculaResumen;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnadirAlCarrito {

    private final CarritoRepository carritos;
    private final LineaCarritoRepository lineas;
    private final CatalogApi peliculas;

    public AnadirAlCarrito(CarritoRepository carritos, LineaCarritoRepository lineas, CatalogApi peliculas) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.peliculas = peliculas;
    }

    @Transactional
    public LineaDeCarrito ejecutar(Long usuarioId, Long peliculaId) {
        PeliculaResumen pelicula = peliculaComprable(peliculaId);
        Carrito carrito = obtenerOCrear(usuarioId);
        if (lineas.existsByCarritoIdAndPeliculaId(carrito.getId(), peliculaId)) {
            throw new ConflictoException("La película ya está en el carrito");
        }
        lineas.save(nuevaLinea(carrito, peliculaId));
        return new LineaDeCarrito(peliculaId, pelicula.titulo(), pelicula.precio());
    }

    /** La película debe existir en el catálogo y tener precio. */
    private PeliculaResumen peliculaComprable(Long peliculaId) {

        PeliculaResumen pelicula = peliculas.resumenes(List.of(peliculaId)).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La película " + peliculaId + " no existe"));

        if (pelicula.precio() == null) {
            throw new ConflictoException("La película no tiene precio y no es comprable");
        }

        return pelicula;
    }

    private static LineaCarrito nuevaLinea(Carrito carrito, Long peliculaId) {
        var linea = new LineaCarrito();
        linea.setCarritoId(carrito.getId());
        linea.setPeliculaId(peliculaId);
        return linea;
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
