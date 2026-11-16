package shop.rating.internal.domain.dto;

import shop.rating.internal.domain.model.MarkType;


/** Presence of a movie in one of a user's lists (favourites or watched); {@code id} is null until saved. */
public record MovieMarkDto(
        Long id,
        Long userId,
        Long movieId,
        MarkType type) {
}
