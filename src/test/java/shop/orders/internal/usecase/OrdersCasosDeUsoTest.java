package shop.orders.internal.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.compartido.error.ConflictoException;
import shop.compartido.error.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Módulo orders aislado: la API del catálogo está doblada a mano. */
class OrdersCasosDeUsoTest {

    private CatalogApiFalsa catalogo;
    private OrderRepositoryFalso pedidos;
    private CreateOrder crear;
    private GetOrder consultar;
    private CancelOrder cancelar;

    @BeforeEach
    void preparar() {
        catalogo = new CatalogApiFalsa();
        catalogo.crearProducto(1L, "Alien", "12.99", 5);
        catalogo.crearProducto(2L, "Matrix", "7.50", 2);
        pedidos = new OrderRepositoryFalso();
        crear = new CreateOrder(catalogo, pedidos);
        consultar = new GetOrder(pedidos);
        cancelar = new CancelOrder(catalogo, pedidos);
    }

    private static NewOrder pedido(NewOrderLine... lineas) {
        return new NewOrder(7L, List.of(lineas));
    }

    @Test
    void crearReservaElStockYPersisteElPedidoConLosPreciosCapturados() {
        OrderView vista = crear.execute(pedido(new NewOrderLine(1L, 2), new NewOrderLine(2L, 1)));

        assertNotNull(vista.id());
        assertEquals("CREATED", vista.status());
        assertEquals(7L, vista.userId());
        assertEquals(3, catalogo.stockDe(1L));
        assertEquals(1, catalogo.stockDe(2L));
        assertEquals(0, new BigDecimal("33.48").compareTo(vista.total()));
        assertEquals(2, vista.lines().size());
        assertEquals(0, new BigDecimal("12.99").compareTo(vista.lines().get(0).unitPrice()));
        assertEquals("Alien", vista.lines().get(0).title());
        assertEquals(vista, consultar.execute(vista.id()));
    }

    @Test
    void elPrecioCapturadoNoCambiaSiElCatalogoCambiaDespues() {
        OrderView vista = crear.execute(pedido(new NewOrderLine(1L, 1)));

        catalogo.cambiarPrecio(1L, "99.00");

        assertEquals(0, new BigDecimal("12.99").compareTo(consultar.execute(vista.id()).lines().get(0).unitPrice()));
    }

    @Test
    void unaLineaSinStockNoDejaNadaReservadoNiCreaElPedido() {
        assertThrows(ConflictoException.class,
                () -> crear.execute(pedido(new NewOrderLine(1L, 2), new NewOrderLine(2L, 3))));

        assertEquals(5, catalogo.stockDe(1L));
        assertEquals(2, catalogo.stockDe(2L));
        assertEquals(0, pedidos.count());
    }

    @Test
    void unProductoDesconocidoDa404YNoReservaNada() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> crear.execute(pedido(new NewOrderLine(1L, 1), new NewOrderLine(99L, 1))));

        assertEquals(5, catalogo.stockDe(1L));
        assertEquals(0, pedidos.count());
    }

    @Test
    void consultarUnPedidoInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.execute(99L));
    }

    @Test
    void cancelarLiberaElStockYPasaACancelled() {
        OrderView vista = crear.execute(pedido(new NewOrderLine(1L, 2), new NewOrderLine(2L, 2)));

        OrderView cancelado = cancelar.execute(vista.id());

        assertEquals("CANCELLED", cancelado.status());
        assertEquals("CANCELLED", consultar.execute(vista.id()).status());
        assertEquals(5, catalogo.stockDe(1L));
        assertEquals(2, catalogo.stockDe(2L));
    }

    @Test
    void cancelarDosVecesFallaConConflictoYNoLiberaDeNuevo() {
        OrderView vista = crear.execute(pedido(new NewOrderLine(1L, 2)));
        cancelar.execute(vista.id());

        assertThrows(ConflictoException.class, () -> cancelar.execute(vista.id()));

        assertEquals(5, catalogo.stockDe(1L));
    }

    @Test
    void cancelarUnPedidoInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> cancelar.execute(99L));
    }
}
