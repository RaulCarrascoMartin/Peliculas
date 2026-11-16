package shop.purchase.internal.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.purchase.internal.domain.dto.CatalogMovieDto;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseLine;

import java.util.List;

/** DTO ↔ model of the purchase domain. Only transforms data: the rules live in the service. */
@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    Purchase toModel(PurchaseDto dto);

    PurchaseDto toDto(Purchase purchase);

    /** A catalog movie becomes a line that copies its title and price; the ids are assigned when saving. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchaseId", ignore = true)
    @Mapping(target = "movieId", source = "id")
    @Mapping(target = "unitPrice", source = "price")
    PurchaseLine toLine(CatalogMovieDto movie);

    List<PurchaseLine> toLines(List<CatalogMovieDto> movies);
}
