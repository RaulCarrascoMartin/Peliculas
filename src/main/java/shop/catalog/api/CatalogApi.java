package shop.catalog.api;

import java.util.Collection;
import java.util.List;

/** API pública del módulo {@code pelicula}: lo único que otros módulos pueden usar. */
public interface CatalogApi {

    boolean existe(Long id);

    /** Guarda la media calculada por otro módulo; lanza 404 si la película no existe. */
    void actualizarValoracionMedia(Long id, Double media);

    /** Datos de las películas pedidas, ordenadas por id; los ids inexistentes se ignoran. */
    List<PeliculaResumen> resumenes(Collection<Long> ids);

    /**
     * Reserva unidades de todas las líneas o de ninguna: si una falla, el stock queda como estaba.
     * Devuelve una línea por cada línea pedida, con el título y el precio del momento.
     *
     * @throws UnknownProductException        si algún producto no existe
     * @throws ProductNotPurchasableException si algún producto no tiene precio
     * @throws InsufficientStockException     si no hay unidades suficientes de algún producto
     */
    List<ReservedLine> reserve(List<StockLine> lines);

    /** Devuelve al stock las unidades indicadas; ignora los productos que ya no existen. */
    void release(List<StockLine> lines);
}
