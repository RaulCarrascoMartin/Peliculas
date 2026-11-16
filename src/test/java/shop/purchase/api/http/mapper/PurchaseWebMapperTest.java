package shop.purchase.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.purchase.api.http.dto.PurchaseLineResponse;
import shop.purchase.api.http.dto.PurchaseResponse;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.model.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurchaseWebMapperTest {

    private static final LocalDateTime PURCHASED_AT = LocalDateTime.of(2026, 11, 2, 10, 15, 30);

    private final PurchaseWebMapper mapper = Mappers.getMapper(PurchaseWebMapper.class);

    private static PurchaseDto purchase(Long id) {
        return new PurchaseDto(id, 7L, PURCHASED_AT, new BigDecimal("17.48"), PurchaseStatus.COMPRADA, List.of(
                new PurchaseLineDto(100L, id, 1L, "Matrix", new BigDecimal("7.99")),
                new PurchaseLineDto(101L, id, 2L, "Inception", new BigDecimal("9.49"))));
    }

    @Test
    void purchaseBecomesTheResponseOfTheContract() {
        PurchaseResponse response = mapper.toResponse(purchase(10L));

        assertEquals(new PurchaseResponse(10L, PURCHASED_AT.atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                PurchaseResponse.EstadoEnum.COMPRADA,
                List.of(new PurchaseLineResponse(1L, "Matrix", new BigDecimal("7.99")),
                        new PurchaseLineResponse(2L, "Inception", new BigDecimal("9.49"))),
                new BigDecimal("17.48")), response);
    }

    @Test
    void theDateIsPublishedWithTheOffsetOfTheServerZoneAndKeepsTheLocalTime() {
        assertEquals(PURCHASED_AT, mapper.toOffset(PURCHASED_AT).toLocalDateTime());
        assertEquals(PURCHASED_AT.atZone(ZoneId.systemDefault()).toInstant(), mapper.toOffset(PURCHASED_AT).toInstant());
        assertNull(mapper.toOffset(null));
    }

    @Test
    void listOfPurchasesKeepsItsOrder() {
        List<PurchaseResponse> responses = mapper.toResponses(List.of(purchase(11L), purchase(10L)));

        assertEquals(List.of(11L, 10L), responses.stream().map(PurchaseResponse::id).toList());
        assertEquals(List.of(), mapper.toResponses(List.of()));
    }
}
