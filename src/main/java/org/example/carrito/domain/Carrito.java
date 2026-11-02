package org.example.carrito.domain;

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
@Table(name = "carritos", uniqueConstraints = @UniqueConstraint(columnNames = "usuarioId"))
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    /** Bloqueo optimista: dos checkouts del mismo carrito, el segundo falla al hacer flush (409). */
    @Version
    private Long version;

    /**
     * Última vez que se vació el carrito. Cambiarlo hace que la entidad esté sucia y que Hibernate emita
     * {@code update ... where version=?}: sin un cambio real, vaciar (que solo borra líneas) nunca tocaría
     * la versión y dos checkouts simultáneos no chocarían.
     */
    private LocalDateTime actualizadoEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
