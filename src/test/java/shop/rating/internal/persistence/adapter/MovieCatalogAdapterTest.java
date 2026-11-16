package shop.rating.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import shop.rating.internal.domain.contract.CatalogMovie;
import shop.shared.CatalogFacadeStub;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieCatalogAdapterTest {

    private final CatalogFacadeStub catalog = new CatalogFacadeStub();
    private final MovieCatalogAdapter adapter = new MovieCatalogAdapter(catalog);

    @Test
    void translatesSummariesAndExistence() {
        assertTrue(adapter.exists(1L));
        assertFalse(adapter.exists(2L));
        assertEquals(List.of(new CatalogMovie(1L, "Alien", 1979, "Horror", "Ridley Scott", 4.5)),
                adapter.findByIds(List.of(1L)));
    }

    @Test
    void delegatesTheAverageUpdate() {
        adapter.updateAverageRating(1L, 3.5);

        assertEquals(List.of("updateAverageRating:1:3.5"), catalog.calls);
    }
}
