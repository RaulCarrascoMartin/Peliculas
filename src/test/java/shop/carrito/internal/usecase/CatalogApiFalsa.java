package shop.carrito.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.PeliculaResumen;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble de prueba de {@link CatalogApi}: recuerda existentes y precios, mutables para el precio vivo. */
public class CatalogApiFalsa implements CatalogApi {

    private final Map<Long, BigDecimal> precios = new HashMap<>();

    public void crearPelicula(Long id, BigDecimal precio) {
        precios.put(id, precio);
    }

    /** Cambia el precio de una película existente, como el administrador de {@code pelicula}. */
    public void cambiarPrecio(Long id, BigDecimal precio) {
        precios.put(id, precio);
    }

    @Override
    public boolean existe(Long id) {
        return precios.containsKey(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        // el carrito nunca actualiza medias; no lo consume este módulo
    }

    /** Datos de las películas pedidas, ordenadas por id; los ids inexistentes se ignoran. */
    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        return ids.stream().filter(this::existe).sorted()
                .map(id -> new PeliculaResumen(id, "Película " + id, 2000, "Drama", "Director",
                        precios.get(id), null, 0))
                .toList();
    }

    @Override
    public java.util.List<shop.catalog.api.ReservedLine> reserve(java.util.List<shop.catalog.api.StockLine> lines) {
        throw new UnsupportedOperationException("No lo usa este módulo");
    }

    @Override
    public void release(java.util.List<shop.catalog.api.StockLine> lines) {
        throw new UnsupportedOperationException("No lo usa este módulo");
    }
}
