package org.example.carrito.internal.usecase;

import org.example.pelicula.api.PeliculaApi;
import org.example.pelicula.api.PeliculaResumen;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble de prueba de {@link PeliculaApi}: recuerda existentes y precios, mutables para el precio vivo. */
public class PeliculaApiFalsa implements PeliculaApi {

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
                        precios.get(id), null))
                .toList();
    }
}
