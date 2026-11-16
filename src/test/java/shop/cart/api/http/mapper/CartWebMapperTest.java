package shop.cart.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.cart.api.http.dto.CartLineResponse;
import shop.cart.api.http.dto.CartResponse;
import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.PricedLineDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartWebMapperTest {

    private final CartWebMapper mapper = Mappers.getMapper(CartWebMapper.class);

    @Test
    void pricedLineBecomesTheLineOfTheContract() {
        assertEquals(new CartLineResponse(10L, "Alien", new BigDecimal("12.99")),
                mapper.toResponse(new PricedLineDto(10L, "Alien", new BigDecimal("12.99"))));
    }

    @Test
    void contentsBecomeTheCartOfTheContractWithItsLinesInOrder() {
        var contents = new CartContentsDto(List.of(
                new PricedLineDto(11L, "Matrix", new BigDecimal("5.00")),
                new PricedLineDto(10L, "Alien", new BigDecimal("15.50"))), new BigDecimal("20.50"));

        assertEquals(new CartResponse(List.of(
                new CartLineResponse(11L, "Matrix", new BigDecimal("5.00")),
                new CartLineResponse(10L, "Alien", new BigDecimal("15.50"))), new BigDecimal("20.50")),
                mapper.toResponse(contents));
    }

    @Test
    void emptyCartHasNoLinesAndZeroTotal() {
        assertEquals(new CartResponse(List.of(), BigDecimal.ZERO),
                mapper.toResponse(new CartContentsDto(List.of(), BigDecimal.ZERO)));
    }
}
