package shop.purchase.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Immutable snapshot of a movie at purchase time: it does not reference the movie, it only copies its data. */
@Entity
@Table(name = "lineas_compra")
public class PurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compra_id", nullable = false)
    private Long purchaseId;

    @Column(name = "pelicula_id", nullable = false)
    private Long movieId;

    @Column(name = "titulo", nullable = false)
    private String title;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
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
}
