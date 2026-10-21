package org.example.valoracion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    Optional<Valoracion> findByUsuarioIdAndPeliculaId(Long usuarioId, Long peliculaId);

    List<Valoracion> findByPeliculaId(Long peliculaId);
}
