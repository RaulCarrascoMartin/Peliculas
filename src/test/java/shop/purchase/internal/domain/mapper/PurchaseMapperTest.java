package shop.purchase.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.purchase.internal.domain.dto.CatalogMovieDto;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.model.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurchaseMapperTest {

    private final PurchaseMapper mapper = Mappers.getMapper(PurchaseMapper.class);

    @Test
    void dtoSurvivesTheRoundTripThroughTheModel() {
        var dto = new PurchaseDto(10L, 7L, LocalDateTime.of(2026, 11, 2, 10, 15, 30), new BigDecimal("17.48"),
                PurchaseStatus.COMPRADA, List.of(
                        new PurchaseLineDto(100L, 10L, 1L, "Matrix", new BigDecimal("7.99")),
                        new PurchaseLineDto(101L, 10L, 2L, "Inception", new BigDecimal("9.49"))));

        Purchase model = mapper.toModel(dto);

        assertEquals(10L, model.getId());
        assertEquals(7L, model.getUserId());
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 15, 30), model.getPurchasedAt());
        assertEquals(new BigDecimal("17.48"), model.getTotal());
        assertEquals(PurchaseStatus.COMPRADA, model.getStatus());
        assertEquals(2, model.getLines().size());
        assertEquals(101L, model.getLines().get(1).getId());
        assertEquals(10L, model.getLines().get(1).getPurchaseId());
        assertEquals(2L, model.getLines().get(1).getMovieId());
        assertEquals("Inception", model.getLines().get(1).getTitle());
        assertEquals(new BigDecimal("9.49"), model.getLines().get(1).getUnitPrice());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void catalogMovieBecomesALineThatCopiesTitleAndPriceWithoutIds() {
        PurchaseLine line = mapper.toLine(new CatalogMovieDto(1L, "Matrix", new BigDecimal("7.99")));

        assertNull(line.getId());
        assertNull(line.getPurchaseId());
        assertEquals(1L, line.getMovieId());
        assertEquals("Matrix", line.getTitle());
        assertEquals(new BigDecimal("7.99"), line.getUnitPrice());
    }

    @Test
    void catalogMoviesBecomeLinesInTheSameOrder() {
        List<PurchaseLine> lines = mapper.toLines(List.of(
                new CatalogMovieDto(2L, "Inception", new BigDecimal("9.49")),
                new CatalogMovieDto(1L, "Matrix", new BigDecimal("7.99"))));

        assertEquals(List.of(2L, 1L), lines.stream().map(PurchaseLine::getMovieId).toList());
    }
}
