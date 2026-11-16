package shop.purchase.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.purchase.api.http.dto.PurchaseLineResponse;
import shop.purchase.api.http.dto.PurchaseResponse;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** DTO of the purchase domain → DTO of the HTTP contract (the contract has no request bodies). */
@Mapper(componentModel = "spring")
public interface PurchaseWebMapper {

    @Mapping(target = "fecha", source = "purchasedAt")
    @Mapping(target = "estado", source = "status")
    @Mapping(target = "lineas", source = "lines")
    PurchaseResponse toResponse(PurchaseDto purchase);

    List<PurchaseResponse> toResponses(List<PurchaseDto> purchases);

    @Mapping(target = "peliculaId", source = "movieId")
    @Mapping(target = "titulo", source = "title")
    @Mapping(target = "precioUnitario", source = "unitPrice")
    PurchaseLineResponse toResponse(PurchaseLineDto line);

    /** The domain holds a {@code LocalDateTime} (server time); the contract publishes {@code date-time} with an offset. */
    default OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
