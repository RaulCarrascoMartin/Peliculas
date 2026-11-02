package shop.catalog.internal.usecase;

import shop.catalog.api.InsufficientStockException;
import shop.catalog.api.ProductNotPurchasableException;
import shop.catalog.api.ReservedLine;
import shop.catalog.api.StockLine;
import shop.catalog.api.UnknownProductException;
import shop.catalog.internal.domain.Pelicula;
import shop.catalog.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserva todas las líneas o ninguna, sin transacción: primero valida todo contra los datos leídos y
 * después descuenta cada producto con una resta condicional; si una de esas restas pierde una carrera,
 * devuelve lo ya descontado antes de fallar.
 */
@Service
public class ReservarStock {

    private final PeliculaRepository peliculas;

    public ReservarStock(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    public List<ReservedLine> ejecutar(List<StockLine> lineas) {
        Map<Long, Integer> pedidoPorProducto = agrupar(lineas);
        Map<Long, Pelicula> existentes = peliculas.findAllById(pedidoPorProducto.keySet()).stream()
                .collect(Collectors.toMap(Pelicula::getId, Function.identity()));
        validar(pedidoPorProducto, existentes);
        descontar(pedidoPorProducto);
        return lineas.stream()
                .map(l -> {
                    Pelicula p = existentes.get(l.productId());
                    return new ReservedLine(p.getId(), p.getTitulo(), p.getPrecio(), l.quantity());
                })
                .toList();
    }

    /** Suma las cantidades de las líneas que repiten producto, conservando el orden de aparición. */
    private static Map<Long, Integer> agrupar(List<StockLine> lineas) {
        Map<Long, Integer> pedido = new LinkedHashMap<>();
        lineas.forEach(l -> pedido.merge(l.productId(), l.quantity(), Integer::sum));
        return pedido;
    }

    private static void validar(Map<Long, Integer> pedido, Map<Long, Pelicula> existentes) {
        pedido.forEach((id, cantidad) -> {
            Pelicula p = existentes.get(id);
            if (p == null) {
                throw new UnknownProductException(id);
            }
            if (p.getPrecio() == null) {
                throw new ProductNotPurchasableException(id);
            }
            if (p.getStock() < cantidad) {
                throw new InsufficientStockException(id);
            }
        });
    }

    private void descontar(Map<Long, Integer> pedido) {
        List<Map.Entry<Long, Integer>> aplicadas = new ArrayList<>();
        for (Map.Entry<Long, Integer> linea : pedido.entrySet()) {
            if (peliculas.descontarStock(linea.getKey(), linea.getValue()) == 0) {
                aplicadas.forEach(a -> peliculas.aumentarStock(a.getKey(), a.getValue()));
                throw new InsufficientStockException(linea.getKey());
            }
            aplicadas.add(linea);
        }
    }
}
