package shop.rating.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "marcas_pelicula",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "pelicula_id", "tipo"}))
public class MovieMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long userId;

    @Column(name = "pelicula_id", nullable = false)
    private Long movieId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private MarkType type;

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

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public MarkType getType() {
        return type;
    }

    public void setType(MarkType type) {
        this.type = type;
    }
}
