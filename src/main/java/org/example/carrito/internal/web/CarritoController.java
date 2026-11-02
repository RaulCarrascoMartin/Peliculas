package org.example.carrito.internal.web;

import org.example.carrito.internal.usecase.AnadirAlCarrito;
import org.example.carrito.internal.usecase.QuitarDelCarrito;
import org.example.carrito.internal.usecase.VaciarCarrito;
import org.example.carrito.internal.usecase.VerCarrito;
import org.example.carrito.internal.usecase.CarritoVista;
import org.example.carrito.internal.usecase.LineaDeCarrito;
import org.example.carrito.internal.web.api.CarritoRestApi;
import org.example.carrito.internal.web.dto.AnadirLinea;
import org.example.carrito.internal.web.dto.Carrito;
import org.example.carrito.internal.web.dto.LineaCarrito;
import org.example.compartido.UsuarioActual;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CarritoController implements CarritoRestApi {

    private final VerCarrito ver;
    private final AnadirAlCarrito anadir;
    private final QuitarDelCarrito quitar;
    private final VaciarCarrito vaciar;
    private final UsuarioActual usuarioActual;

    public CarritoController(VerCarrito ver, AnadirAlCarrito anadir, QuitarDelCarrito quitar,
                             VaciarCarrito vaciar, UsuarioActual usuarioActual) {
        this.ver = ver;
        this.anadir = anadir;
        this.quitar = quitar;
        this.vaciar = vaciar;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public Carrito verCarrito() {
        CarritoVista vista = ver.ejecutar(usuarioActual.id());
        return mapear(vista);
    }

    @Override
    public LineaCarrito anadirLinea(AnadirLinea anadirLinea) {
        LineaDeCarrito linea = anadir.ejecutar(usuarioActual.id(), anadirLinea.peliculaId());
        return mapear(linea);
    }

    @Override
    public void quitarLinea(Long peliculaId) {
        quitar.ejecutar(usuarioActual.id(), peliculaId);
    }

    @Override
    public void vaciarCarrito() {
        vaciar.ejecutar(usuarioActual.id());
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
