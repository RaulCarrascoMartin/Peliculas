package shop.cart.internal.domain.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.dto.CatalogMovieDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CartMapperTest {

    private final CartMapper mapper = Mappers.getMapper(CartMapper.class);

    @Test
    void cartSurvivesTheRoundTripThroughTheModel() {
        var dto = new CartDto(5L, 7L, 3L, LocalDateTime.of(2026, 11, 2, 10, 15, 30));

        Cart model = mapper.toModel(dto);

        assertEquals(5L, model.getId());
        assertEquals(7L, model.getUserId());
        assertEquals(3L, model.getVersion());
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 15, 30), model.getUpdatedAt());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void newCartKeepsItsEmptyFields() {
        var cart = new Cart();
        cart.setUserId(7L);

        assertEquals(new CartDto(null, 7L, null, null), mapper.toDto(cart));
        assertNull(mapper.toModel(new CartDto(null, 7L, null, null)).getId());
    }

    @Test
    void lineSurvivesTheRoundTripThroughTheModel() {
        var dto = new CartLineDto(9L, 5L, 10L);

        CartLine model = mapper.toModel(dto);

        assertEquals(9L, model.getId());
        assertEquals(5L, model.getCartId());
        assertEquals(10L, model.getMovieId());
        assertEquals(dto, mapper.toDto(model));
    }

    @Test
    void catalogMovieBecomesAPricedLine() {
        assertEquals(new PricedLineDto(10L, "Alien", new BigDecimal("12.99")),
                mapper.toPricedLine(new CatalogMovieDto(10L, "Alien", new BigDecimal("12.99"))));
    }
}
