package shop.orders.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.PeliculaResumen;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble de {@link CatalogApi} para probar {@code orders} aislado: productos con título, precio y stock. */
public class CatalogApiFalsa implements CatalogApi {

    private final Map<Long, String> titulos = new HashMap<>();
    private final Map<Long, BigDecimal> precios = new HashMap<>();
    private final Map<Long, Integer> stock = new HashMap<>();

    public void crearProducto(Long id, String titulo, String precio, int unidades) {
        titulos.put(id, titulo);
        precios.put(id, new BigDecimal(precio));
        stock.put(id, unidades);
    }

    public void cambiarPrecio(Long id, String precio) {
        precios.put(id, new BigDecimal(precio));
    }

    public int stockDe(Long id) {
        return stock.get(id);
    }

    @Override
    public List<ReservedLine> reserve(List<StockLine> lines) {
        // Todo o nada: se valida antes de descontar nada.
        for (StockLine l : lines) {
            if (!stock.containsKey(l.productId())) {
                throw new UnknownProductException(l.productId());
            }
        }
        Map<Long, Integer> pedido = new HashMap<>();
        lines.forEach(l -> pedido.merge(l.productId(), l.quantity(), Integer::sum));
        pedido.forEach((id, cantidad) -> {
            if (stock.get(id) < cantidad) {
                throw new InsufficientStockException(id);
            }
        });
        pedido.forEach((id, cantidad) -> stock.merge(id, -cantidad, Integer::sum));
        return lines.stream()
                .map(l -> new ReservedLine(l.productId(), titulos.get(l.productId()), precios.get(l.productId()),
                        l.quantity()))
                .toList();
    }

    @Override
    public void release(List<StockLine> lines) {
        lines.forEach(l -> stock.computeIfPresent(l.productId(), (id, actual) -> actual + l.quantity()));
    }

    @Override
    public boolean existe(Long id) {
        throw new UnsupportedOperationException("orders solo usa reserve y release");
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        throw new UnsupportedOperationException("orders solo usa reserve y release");
    }

    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        throw new UnsupportedOperationException("orders solo usa reserve y release");
    }
}
