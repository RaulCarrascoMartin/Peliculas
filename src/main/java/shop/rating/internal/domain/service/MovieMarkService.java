package shop.rating.internal.domain.service;

import shop.rating.internal.domain.dto.CatalogMovieDto;
import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieNotFoundException;
import shop.rating.internal.domain.model.MarkType;

import java.util.List;

/** Favourite / watched lists of a user. */
public interface MovieMarkService {

    /**
     * Adds the movie to the user's list; if it was already there, does nothing.
     *
     * @throws MovieNotFoundException if the movie does not exist
     */
    void mark(MovieMarkDto mark);

    /** Removes the movie from the user's list; if it was not there (or the movie does not exist), does nothing. */
    void unmark(MovieMarkDto mark);

    /** Movies in the user's list with their catalog data, sorted by id; movies that no longer exist are left out. */
    List<CatalogMovieDto> listMarked(Long userId, MarkType type);
}
