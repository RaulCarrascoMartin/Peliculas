package org.example.valoracion.internal.usecase;

import org.example.compartido.RepositorioEnMemoria;
import org.example.valoracion.internal.domain.MarcaPelicula;
import org.example.valoracion.internal.domain.MarcaPeliculaRepository;
import org.example.valoracion.internal.domain.TipoMarca;

import java.util.List;
import java.util.Optional;

public class MarcaPeliculaRepositoryFalso extends RepositorioEnMemoria<MarcaPelicula>
        implements MarcaPeliculaRepository {

    @Override
    protected Long idDe(MarcaPelicula marca) {
        return marca.getId();
    }

    @Override
    protected void asignarId(MarcaPelicula marca, Long id) {
        marca.setId(id);
    }

    @Override
    public Optional<MarcaPelicula> findByUsuarioIdAndPeliculaIdAndTipo(Long usuarioId, Long peliculaId, TipoMarca tipo) {
        return entidades.values().stream()
                .filter(m -> m.getUsuarioId().equals(usuarioId) && m.getPeliculaId().equals(peliculaId)
                        && m.getTipo() == tipo)
                .findFirst();
    }

    @Override
    public List<MarcaPelicula> findByUsuarioIdAndTipo(Long usuarioId, TipoMarca tipo) {
        return entidades.values().stream()
                .filter(m -> m.getUsuarioId().equals(usuarioId) && m.getTipo() == tipo)
                .toList();
    }

    @Override
    public void deleteByPeliculaId(Long peliculaId) {
        entidades.values().removeIf(m -> m.getPeliculaId().equals(peliculaId));
    }
}
