package shop.cart.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.cart.api.http.dto.CartLineResponse;
import shop.cart.api.http.dto.CartResponse;
import shop.cart.internal.domain.dto.CartContentsDto;
import shop.cart.internal.domain.dto.PricedLineDto;

/** DTO of the cart domain → DTO of the HTTP contract (whose property names are the Spanish wire names). */
@Mapper(componentModel = "spring")
public interface CartWebMapper {

    @Mapping(target = "lineas", source = "lines")
    CartResponse toResponse(CartContentsDto contents);

    @Mapping(target = "peliculaId", source = "movieId")
    @Mapping(target = "titulo", source = "title")
    @Mapping(target = "precio", source = "price")
    CartLineResponse toResponse(PricedLineDto line);
}
