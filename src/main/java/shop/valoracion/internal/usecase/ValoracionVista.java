package shop.valoracion.internal.usecase;

/** Valoración de un usuario sobre una película, tal como la devuelve el caso de uso de valorar. */
public record ValoracionVista(
        Long id,
        Long peliculaId,
        Integer puntuacion,
        String comentario) {
}
