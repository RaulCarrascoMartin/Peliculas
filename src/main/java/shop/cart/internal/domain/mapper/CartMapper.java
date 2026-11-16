package shop.cart.internal.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.cart.internal.domain.dto.CartDto;
import shop.cart.internal.domain.dto.CartLineDto;
import shop.cart.internal.domain.dto.CatalogMovieDto;
import shop.cart.internal.domain.dto.PricedLineDto;
import shop.cart.internal.domain.model.Cart;
import shop.cart.internal.domain.model.CartLine;

/** DTO ↔ model of the cart domain. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface CartMapper {

    Cart toModel(CartDto dto);

    CartDto toDto(Cart cart);

    CartLine toModel(CartLineDto dto);

    CartLineDto toDto(CartLine line);

    /** A line shows the title and live price of its movie in the catalog. */
    @Mapping(target = "movieId", source = "id")
    PricedLineDto toPricedLine(CatalogMovieDto movie);
}
