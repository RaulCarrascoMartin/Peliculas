package shop.purchase.internal.domain.dto;

import shop.purchase.internal.domain.model.PurchaseStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Purchase with its frozen lines as it travels between layers: controller, service and repository. */
public record PurchaseDto(
        Long id,
        Long userId,
        LocalDateTime purchasedAt,
        BigDecimal total,
        PurchaseStatus status,
        List<PurchaseLineDto> lines) {
}
