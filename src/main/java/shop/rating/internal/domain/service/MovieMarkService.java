package shop.rating.internal.domain.service;

import shop.rating.internal.domain.contract.CatalogMovie;
import shop.rating.internal.domain.model.MarkType;

import java.util.List;

/** Favourite / watched lists of a user. */
public interface MovieMarkService {

    /** Adds the movie to the user's list; if it was already there, does nothing. */
    void mark(Long userId, Long movieId, MarkType type);

    /** Removes the movie from the user's list; if it was not there, does nothing. */
    void unmark(Long userId, Long movieId, MarkType type);

    /** Movies in the user's list, sorted by id. */
    List<CatalogMovie> listMarked(Long userId, MarkType type);
}
