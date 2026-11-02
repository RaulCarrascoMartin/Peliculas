package shop.valoracion.internal.usecase;

import shop.compartido.seguridad.UsuarioActual;
import shop.valoracion.internal.domain.MarcaPeliculaRepository;
import shop.valoracion.internal.domain.TipoMarca;
import org.springframework.stereotype.Service;

@Service
public class DesmarcarPelicula {

    private final MarcaPeliculaRepository marcas;
    private final UsuarioActual usuarioActual;

    public DesmarcarPelicula(MarcaPeliculaRepository marcas, UsuarioActual usuarioActual) {
        this.marcas = marcas;
        this.usuarioActual = usuarioActual;
    }

    /** Quita la película de la lista; si no estaba, no hace nada. */
    public void ejecutar(Long peliculaId, TipoMarca tipo) {
        marcas.findByUsuarioIdAndPeliculaIdAndTipo(usuarioActual.id(), peliculaId, tipo)
                .ifPresent(marcas::delete);
    }
}
