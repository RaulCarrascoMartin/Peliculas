package shop.compra.internal.usecase;

import shop.catalog.api.CatalogApi;
import shop.catalog.api.PeliculaResumen;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble de prueba de {@link CatalogApi}: títulos y precios mutables, como los edita el administrador. */
public class CatalogApiFalsa implements CatalogApi {

    private final Map<Long, String> titulos = new HashMap<>();
    private final Map<Long, BigDecimal> precios = new HashMap<>();

    public void crearPelicula(Long id, String titulo, BigDecimal precio) {
        titulos.put(id, titulo);
        precios.put(id, precio);
    }

    public void cambiarPrecio(Long id, BigDecimal precio) {
        precios.put(id, precio);
    }

    public void cambiarTitulo(Long id, String titulo) {
        titulos.put(id, titulo);
    }

    public void eliminar(Long id) {
        titulos.remove(id);
        precios.remove(id);
    }

    @Override
    public boolean existe(Long id) {
        return titulos.containsKey(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        // la compra nunca actualiza medias; no lo consume este módulo
    }

    @Override
    public List<PeliculaResumen> resumenes(Collection<Long> ids) {
        return ids.stream().filter(this::existe).sorted()
                .map(id -> new PeliculaResumen(id, titulos.get(id), 2000, "Drama", "Director",
                        precios.get(id), null))
                .toList();
    }
}
