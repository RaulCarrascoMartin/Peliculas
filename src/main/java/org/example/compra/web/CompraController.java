package org.example.compra.web;

import org.example.compartido.UsuarioActual;
import org.example.compra.application.CompraService;
import org.example.compra.application.CompraVista;
import org.example.compra.domain.LineaCompra;
import org.example.compra.web.api.ComprasRestApi;
import org.example.compra.web.dto.Compra;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

@RestController
public class CompraController implements ComprasRestApi {

    private final CompraService compraService;
    private final UsuarioActual usuarioActual;

    public CompraController(CompraService compraService, UsuarioActual usuarioActual) {
        this.compraService = compraService;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public Compra finalizarCompra() {
        return mapear(compraService.finalizar(usuarioActual.id()));
    }

    @Override
    public List<Compra> listarCompras() {
        return compraService.listar(usuarioActual.id()).stream()
                .map(CompraController::mapear)
                .toList();
    }

    @Override
    public Compra detalleCompra(Long compraId) {
        return mapear(compraService.detalle(usuarioActual.id(), compraId));
    }

    /** La entidad guarda {@code LocalDateTime} (hora del servidor); el contrato publica {@code date-time} con desfase. */
    private static Compra mapear(CompraVista vista) {
        org.example.compra.domain.Compra compra = vista.compra();
        List<org.example.compra.web.dto.LineaCompra> lineas = vista.lineas().stream()
                .map(CompraController::mapear)
                .toList();
        return new Compra(compra.getId(), compra.getFecha().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                Compra.EstadoEnum.valueOf(compra.getEstado().name()), lineas, compra.getTotal());
    }

    private static org.example.compra.web.dto.LineaCompra mapear(LineaCompra linea) {
        return new org.example.compra.web.dto.LineaCompra(linea.getPeliculaId(), linea.getTitulo(),
                linea.getPrecioUnitario());
    }
}
