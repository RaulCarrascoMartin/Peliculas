package shop.purchase.internal.persistence.adapter;

import org.junit.jupiter.api.Test;
import shop.purchase.internal.domain.dto.CatalogMovieDto;
import shop.shared.CatalogFacadeStub;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The adapter translates the catalog's facade types to the DTO of the purchase domain. */
class MovieCatalogAdapterTest {

    private final MovieCatalogAdapter adapter = new MovieCatalogAdapter(new CatalogFacadeStub());

    @Test
    void translatesCatalogSummariesToTheModuleContract() {
        assertEquals(List.of(new CatalogMovieDto(1L, "Alien", new BigDecimal("9.99"))),
                adapter.findByIds(List.of(1L, 2L)));
    }

    @Test
    void unknownIdsAreIgnored() {
        assertEquals(List.of(), adapter.findByIds(List.of(2L)));
    }
}
