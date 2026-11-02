package shop.compra.internal.usecase;

import shop.compartido.error.RecursoNoEncontradoException;
import shop.compra.internal.domain.Compra;
import shop.compra.internal.domain.CompraRepository;
import shop.compra.internal.domain.LineaCompraRepository;
import org.springframework.stereotype.Service;

@Service
public class ObtenerCompra {

    private final CompraRepository compras;
    private final LineaCompraRepository lineas;

    public ObtenerCompra(CompraRepository compras, LineaCompraRepository lineas) {
        this.compras = compras;
        this.lineas = lineas;
    }

    /** Una compra ajena es indistinguible de una inexistente: 404, nunca 403. */
    public CompraVista ejecutar(Long usuarioId, Long compraId) {
        Compra compra = compras.findByIdAndUsuarioId(compraId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La compra " + compraId + " no existe"));
        return VistaDeCompra.de(compra, lineas.findByCompraIdOrderById(compra.getId()));
    }
}
