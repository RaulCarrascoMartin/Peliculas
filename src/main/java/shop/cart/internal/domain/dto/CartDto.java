package shop.cart.internal.domain.dto;

import java.time.LocalDateTime;

/**
 * Cart row as it travels between the service and the repository. {@code version} is the optimistic lock and
 * {@code updatedAt} the last time the cart was cleared; the lines travel apart as {@link CartLineDto}.
 */
public record CartDto(
        Long id,
        Long userId,
        Long version,
        LocalDateTime updatedAt) {
}
