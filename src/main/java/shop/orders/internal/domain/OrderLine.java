package shop.orders.internal.domain;

import java.math.BigDecimal;

/** Línea de pedido; {@code unitPrice} es el precio capturado al reservar, no el vigente del catálogo. */
public class OrderLine {

    private Long productId;
    private String title;
    private BigDecimal unitPrice;
    private int quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
