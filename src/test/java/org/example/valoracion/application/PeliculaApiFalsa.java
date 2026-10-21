package org.example.valoracion.application;

import org.example.compartido.RecursoNoEncontradoException;
import org.example.pelicula.PeliculaApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Doble de prueba de {@link PeliculaApi}: recuerda las películas "existentes" y las medias guardadas. */
public class PeliculaApiFalsa implements PeliculaApi {

    private final Set<Long> existentes = new HashSet<>();
    private final Map<Long, Double> medias = new HashMap<>();

    public void anadirPelicula(Long id) {
        existentes.add(id);
    }

    public Double mediaDe(Long id) {
        return medias.get(id);
    }

    @Override
    public boolean existe(Long id) {
        return existentes.contains(id);
    }

    @Override
    public void actualizarValoracionMedia(Long id, Double media) {
        if (!existe(id)) {
            throw new RecursoNoEncontradoException("Película " + id + " no encontrada");
        }
        medias.put(id, media);
    }
}
