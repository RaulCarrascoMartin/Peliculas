package shop.cart.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "carritos", uniqueConstraints = @UniqueConstraint(columnNames = "usuario_id"))
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long userId;

    /** Optimistic lock: two checkouts of the same cart, the second fails on flush (409). */
    @Version
    private Long version;

    /**
     * Last time the cart was cleared. Changing it makes the entity dirty so Hibernate emits
     * {@code update ... where version=?}: without a real change, clearing (which only deletes lines) would never touch
     * the version and two simultaneous checkouts would not collide.
     */
    @Column(name = "actualizado_en")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
