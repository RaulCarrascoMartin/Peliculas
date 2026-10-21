package org.example.valoracion.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.compartido.UsuarioActual;
import org.example.compartido.ValidacionException;
import org.example.pelicula.PeliculaApi;
import org.example.valoracion.domain.Valoracion;
import org.example.valoracion.domain.ValoracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValoracionService {

    static final int PUNTUACION_MINIMA = 1;
    static final int PUNTUACION_MAXIMA = 5;

    private final ValoracionRepository valoraciones;
    private final PeliculaApi peliculas;
    private final UsuarioActual usuarioActual;

    public ValoracionService(ValoracionRepository valoraciones, PeliculaApi peliculas, UsuarioActual usuarioActual) {
        this.valoraciones = valoraciones;
        this.peliculas = peliculas;
        this.usuarioActual = usuarioActual;
    }

    /** Crea la valoración del usuario actual o, si ya existía para esa película, la actualiza. */
    @Transactional
    public Valoracion valorar(Long peliculaId, int puntuacion, String comentario) {
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
        return guardada;
    }

    private void recalcularMedia(Long peliculaId) {
        double media = valoraciones.findByPeliculaId(peliculaId).stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElseThrow();
        peliculas.actualizarValoracionMedia(peliculaId, media);
    }
}
