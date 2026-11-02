package org.example.valoracion.internal.usecase;

import org.example.compartido.error.RecursoNoEncontradoException;
import org.example.compartido.seguridad.UsuarioActual;
import org.example.compartido.error.ValidacionException;
import org.example.pelicula.api.PeliculaApi;
import org.example.valoracion.internal.domain.Valoracion;
import org.example.valoracion.internal.domain.ValoracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValorarPelicula {

    private final ValoracionRepository valoraciones;
    private final PeliculaApi peliculas;
    private final UsuarioActual usuarioActual;

    public ValorarPelicula(ValoracionRepository valoraciones, PeliculaApi peliculas, UsuarioActual usuarioActual) {
        this.valoraciones = valoraciones;
        this.peliculas = peliculas;
        this.usuarioActual = usuarioActual;
    }

    static final int PUNTUACION_MINIMA = 1;
    static final int PUNTUACION_MAXIMA = 5;

    /** Crea la valoración del usuario actual o, si ya existía para esa película, la actualiza. */
    @Transactional
    public ValoracionVista ejecutar(Long peliculaId, int puntuacion, String comentario) {
        if (puntuacion < PUNTUACION_MINIMA || puntuacion > PUNTUACION_MAXIMA) {
            throw new ValidacionException(
                    "La puntuación debe estar entre " + PUNTUACION_MINIMA + " y " + PUNTUACION_MAXIMA);
        }
        if (!peliculas.existe(peliculaId)) {
            throw new RecursoNoEncontradoException("Película " + peliculaId + " no encontrada");
        }
        Long usuarioId = usuarioActual.id();
        Valoracion valoracion = valoraciones.findByUsuarioIdAndPeliculaId(usuarioId, peliculaId)
                .orElseGet(() -> {
                    Valoracion nueva = new Valoracion();
                    nueva.setUsuarioId(usuarioId);
                    nueva.setPeliculaId(peliculaId);
                    return nueva;
                });
        valoracion.setPuntuacion(puntuacion);
        valoracion.setComentario(comentario);
        Valoracion guardada = valoraciones.save(valoracion);
        recalcularMedia(peliculaId);
        return new ValoracionVista(guardada.getId(), guardada.getPeliculaId(), guardada.getPuntuacion(),
                guardada.getComentario());
    }

    private void recalcularMedia(Long peliculaId) {
        double media = valoraciones.findByPeliculaId(peliculaId).stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElseThrow();
        peliculas.actualizarValoracionMedia(peliculaId, media);
    }
}
