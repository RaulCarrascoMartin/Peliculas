package shop.cart.internal.domain.dto;

import java.math.BigDecimal;
import java.util.List;

/** Current state of a user's cart: lines with live price and total. */
public record CartContentsDto(
        List<PricedLineDto> lines,
        BigDecimal total) {
}
