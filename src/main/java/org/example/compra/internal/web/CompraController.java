package org.example.compra.internal.web;

import org.example.compartido.UsuarioActual;
import org.example.compra.internal.usecase.FinalizarCompra;
import org.example.compra.internal.usecase.ListarCompras;
import org.example.compra.internal.usecase.ObtenerCompra;
import org.example.compra.internal.usecase.CompraVista;
import org.example.compra.internal.domain.LineaCompra;
import org.example.compra.internal.web.api.ComprasRestApi;
import org.example.compra.internal.web.dto.Compra;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

@RestController
public class CompraController implements ComprasRestApi {

    private final FinalizarCompra finalizar;
    private final ListarCompras listar;
    private final ObtenerCompra obtener;
    private final UsuarioActual usuarioActual;

    public CompraController(FinalizarCompra finalizar, ListarCompras listar, ObtenerCompra obtener,
                            UsuarioActual usuarioActual) {
        this.finalizar = finalizar;
        this.listar = listar;
        this.obtener = obtener;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public Compra finalizarCompra() {
        return mapear(finalizar.ejecutar(usuarioActual.id()));
    }

    @Override
    public List<Compra> listarCompras() {
        return listar.ejecutar(usuarioActual.id()).stream()
                .map(CompraController::mapear)
                .toList();
    }

    @Override
    public Compra detalleCompra(Long compraId) {
        return mapear(obtener.ejecutar(usuarioActual.id(), compraId));
    }

    /** La entidad guarda {@code LocalDateTime} (hora del servidor); el contrato publica {@code date-time} con desfase. */
    private static Compra mapear(CompraVista vista) {
        org.example.compra.internal.domain.Compra compra = vista.compra();
        List<org.example.compra.internal.web.dto.LineaCompra> lineas = vista.lineas().stream()
                .map(CompraController::mapear)
                .toList();
        return new Compra(compra.getId(), compra.getFecha().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                Compra.EstadoEnum.valueOf(compra.getEstado().name()), lineas, compra.getTotal());
    }

    private static org.example.compra.internal.web.dto.LineaCompra mapear(LineaCompra linea) {
        return new org.example.compra.internal.web.dto.LineaCompra(linea.getPeliculaId(), linea.getTitulo(),
                linea.getPrecioUnitario());
    }
}
