package org.example.valoracion.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarcaPeliculaRepository extends JpaRepository<MarcaPelicula, Long> {

    Optional<MarcaPelicula> findByUsuarioIdAndPeliculaIdAndTipo(Long usuarioId, Long peliculaId, TipoMarca tipo);

    List<MarcaPelicula> findByUsuarioIdAndTipo(Long usuarioId, TipoMarca tipo);

    void deleteByPeliculaId(Long peliculaId);
}
