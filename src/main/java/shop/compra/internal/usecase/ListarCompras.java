package shop.compra.internal.usecase;

import shop.compra.internal.domain.CompraRepository;
import shop.compra.internal.domain.LineaCompraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarCompras {

    private final CompraRepository compras;
    private final LineaCompraRepository lineas;

    public ListarCompras(CompraRepository compras, LineaCompraRepository lineas) {
        this.compras = compras;
        this.lineas = lineas;
    }

    public List<CompraVista> ejecutar(Long usuarioId) {
        return compras.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(compra -> VistaDeCompra.de(compra, lineas.findByCompraIdOrderById(compra.getId())))
                .toList();
    }
}
