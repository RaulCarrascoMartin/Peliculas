package shop.cart.internal.application.dto;

import java.math.BigDecimal;
import java.util.List;

/** Current state of a user's cart: lines with live price and total. */
public record CartView(
        List<CartLineView> lines,
        BigDecimal total) {
}
