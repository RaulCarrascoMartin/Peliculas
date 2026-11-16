package shop.orders.api.http.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import shop.orders.api.http.dto.CreateOrderRequest;
import shop.orders.api.http.dto.OrderResponse;
import shop.orders.internal.domain.dto.CreateOrderDto;
import shop.orders.internal.domain.dto.OrderDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** DTO of the HTTP contract ↔ DTO of the orders domain. */
@Mapper(componentModel = "spring")
public interface OrderWebMapper {

    /** The user comes from the token, never from the request. */
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "lines", source = "request.lines")
    CreateOrderDto toDto(Long userId, CreateOrderRequest request);

    OrderResponse toResponse(OrderDto order);

    /** The contract publishes instants as date-time in UTC. */
    default OffsetDateTime toUtc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
