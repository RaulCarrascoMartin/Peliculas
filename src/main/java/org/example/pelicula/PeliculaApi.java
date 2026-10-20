package org.example.pelicula;

/** API pública del módulo {@code pelicula}: lo único que otros módulos pueden usar. */
public interface PeliculaApi {

    boolean existe(Long id);

    /** Guarda la media calculada por otro módulo; lanza 404 si la película no existe. */
    void actualizarValoracionMedia(Long id, Double media);
}
