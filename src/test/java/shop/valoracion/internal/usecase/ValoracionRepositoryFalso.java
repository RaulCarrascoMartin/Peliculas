package shop.valoracion.internal.usecase;

import shop.compartido.RepositorioEnMemoria;
import shop.valoracion.internal.domain.Valoracion;
import shop.valoracion.internal.domain.ValoracionRepository;

import java.util.List;
import java.util.Optional;

public class ValoracionRepositoryFalso extends RepositorioEnMemoria<Valoracion> implements ValoracionRepository {

    @Override
    protected Long idDe(Valoracion valoracion) {
        return valoracion.getId();
    }

    @Override
    protected void asignarId(Valoracion valoracion, Long id) {
        valoracion.setId(id);
    }

    @Override
    public Optional<Valoracion> findByUsuarioIdAndPeliculaId(Long usuarioId, Long peliculaId) {
        return entidades.values().stream()
                .filter(v -> v.getUsuarioId().equals(usuarioId) && v.getPeliculaId().equals(peliculaId))
                .findFirst();
    }

    @Override
    public List<Valoracion> findByPeliculaId(Long peliculaId) {
        return entidades.values().stream().filter(v -> v.getPeliculaId().equals(peliculaId)).toList();
    }

    @Override
    public void deleteByPeliculaId(Long peliculaId) {
        entidades.values().removeIf(v -> v.getPeliculaId().equals(peliculaId));
    }
}
