package org.example.carrito.web;

import org.example.carrito.application.CarritoService;
import org.example.carrito.application.CarritoVista;
import org.example.carrito.application.LineaDeCarrito;
import org.example.carrito.web.api.CarritoRestApi;
import org.example.carrito.web.dto.AnadirLinea;
import org.example.carrito.web.dto.Carrito;
import org.example.carrito.web.dto.LineaCarrito;
import org.example.compartido.UsuarioActual;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CarritoController implements CarritoRestApi {

    private final CarritoService carritoService;
    private final UsuarioActual usuarioActual;

    public CarritoController(CarritoService carritoService, UsuarioActual usuarioActual) {
        this.carritoService = carritoService;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public Carrito verCarrito() {
        CarritoVista vista = carritoService.ver(usuarioActual.id());
        return mapear(vista);
    }

    @Override
    public LineaCarrito anadirLinea(AnadirLinea anadirLinea) {
        LineaDeCarrito linea = carritoService.anadir(usuarioActual.id(), anadirLinea.peliculaId());
        return mapear(linea);
    }

    @Override
    public void quitarLinea(Long peliculaId) {
        carritoService.quitar(usuarioActual.id(), peliculaId);
    }

    @Override
    public void vaciarCarrito() {
        carritoService.vaciar(usuarioActual.id());
    }

    private Carrito mapear(CarritoVista vista) {
        List<LineaCarrito> lineas = vista.lineas().stream()
                .map(CarritoController::mapear)
                .toList();
        return new Carrito(lineas, vista.total());
    }

    private static LineaCarrito mapear(LineaDeCarrito linea) {
        return new LineaCarrito(linea.peliculaId(), linea.titulo(), linea.precio());
    }
}
