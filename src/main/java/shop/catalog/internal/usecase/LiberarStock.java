package shop.catalog.internal.usecase;

import shop.catalog.api.StockLine;
import shop.catalog.internal.domain.PeliculaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiberarStock {

    private final PeliculaRepository peliculas;

    public LiberarStock(PeliculaRepository peliculas) {
        this.peliculas = peliculas;
    }

    /** Devuelve las unidades al stock; un producto que ya no existe no afecta a ninguna fila y se ignora. */
    public void ejecutar(List<StockLine> lineas) {
        lineas.forEach(l -> peliculas.aumentarStock(l.productId(), l.quantity()));
    }
}
