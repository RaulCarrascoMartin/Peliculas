package org.example.compra.application;

import org.example.carrito.CarritoApi;
import org.example.compartido.ConflictoException;
import org.example.compartido.RecursoNoEncontradoException;
import org.example.compra.domain.Compra;
import org.example.compra.domain.CompraRepository;
import org.example.compra.domain.EstadoCompra;
import org.example.compra.domain.LineaCompra;
import org.example.compra.domain.LineaCompraRepository;
import org.example.pelicula.PeliculaApi;
import org.example.pelicula.PeliculaResumen;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checkout atómico con snapshot de título y precio, e historial propio. Lee y vacía el carrito solo
 * por {@link CarritoApi} y las películas solo por {@link PeliculaApi}.
 */
@Service
public class CompraService {

    private final CompraRepository compras;
    private final LineaCompraRepository lineas;
    private final CarritoApi carritos;
    private final PeliculaApi peliculas;

    public CompraService(CompraRepository compras, LineaCompraRepository lineas,
                         CarritoApi carritos, PeliculaApi peliculas) {
        this.compras = compras;
        this.lineas = lineas;
        this.carritos = carritos;
        this.peliculas = peliculas;
    }

    /**
     * Convierte el carrito en una compra y lo vacía en la misma transacción. Se valida todo antes de
     * escribir, de modo que un conflicto deja el carrito intacto. El bloqueo optimista del carrito suele
     * saltar en el commit, fuera de este método, y lo traduce {@code GlobalExceptionHandler}; aquí solo se
     * captura si el flush ocurre dentro.
     */
    @Transactional
    public CompraVista finalizar(Long usuarioId) {
        try {
            return crearCompra(usuarioId);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictoException("El carrito ya se está finalizando en otra petición");
        }
    }

    private CompraVista crearCompra(Long usuarioId) {
        List<Long> ids = carritos.peliculasEnCarrito(usuarioId);
        if (ids.isEmpty()) {
            throw new ConflictoException("El carrito está vacío");
        }
        Map<Long, PeliculaResumen> resumenPorId = peliculas.resumenes(ids).stream()
                .collect(Collectors.toMap(PeliculaResumen::id, Function.identity()));
        List<PeliculaResumen> comprables = ids.stream().map(id -> comprable(resumenPorId.get(id), id)).toList();

        BigDecimal total = comprables.stream()
                .map(PeliculaResumen::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var compra = new Compra();
        compra.setUsuarioId(usuarioId);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(total);
        compra.setEstado(EstadoCompra.COMPRADA);
        Compra guardada = compras.save(compra);

        List<LineaCompra> congeladas = comprables.stream()
                .map(resumen -> lineas.save(snapshot(guardada.getId(), resumen)))
                .toList();
        carritos.vaciar(usuarioId);
        return new CompraVista(guardada, congeladas);
    }

    private static PeliculaResumen comprable(PeliculaResumen resumen, Long peliculaId) {
        if (resumen == null) {
            throw new ConflictoException("La película " + peliculaId + " ya no está en el catálogo");
        }
        if (resumen.precio() == null) {
            throw new ConflictoException("La película " + peliculaId + " no tiene precio y no es comprable");
        }
        return resumen;
    }

    private static LineaCompra snapshot(Long compraId, PeliculaResumen resumen) {
        var linea = new LineaCompra();
        linea.setCompraId(compraId);
        linea.setPeliculaId(resumen.id());
        linea.setTitulo(resumen.titulo());
        linea.setPrecioUnitario(resumen.precio());
        return linea;
    }

    @Transactional(readOnly = true)
    public List<CompraVista> listar(Long usuarioId) {
        return compras.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(compra -> new CompraVista(compra, lineas.findByCompraIdOrderById(compra.getId())))
                .toList();
    }

    /** Una compra ajena es indistinguible de una inexistente: 404, nunca 403. */
    @Transactional(readOnly = true)
    public CompraVista detalle(Long usuarioId, Long compraId) {
        Compra compra = compras.findByIdAndUsuarioId(compraId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La compra " + compraId + " no existe"));
        return new CompraVista(compra, lineas.findByCompraIdOrderById(compra.getId()));
    }
}
