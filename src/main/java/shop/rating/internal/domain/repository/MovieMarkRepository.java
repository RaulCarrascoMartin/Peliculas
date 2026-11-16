package shop.rating.internal.domain.repository;

import shop.rating.internal.domain.dto.MovieMarkDto;
import shop.rating.internal.domain.exception.MovieAlreadyMarkedException;
import shop.rating.internal.domain.model.MarkType;

import java.util.List;
import java.util.Optional;

/** Movie mark persistence; the implementation uses JDBC and lives in {@code internal.persistence}. */
public interface MovieMarkRepository {

    /**
     * Inserts the mark (no id) or updates it (with id) and returns it with its id.
     *
     * @throws MovieAlreadyMarkedException if the user already has that movie in that list
     */
    MovieMarkDto save(MovieMarkDto mark);

    /** Deletes the mark with that id; if it does not exist, does nothing. */
    void delete(Long id);

    Optional<MovieMarkDto> findByUserIdAndMovieIdAndType(Long userId, Long movieId, MarkType type);

    /** Marks of the user in that list, sorted by id. */
    List<MovieMarkDto> findByUserIdAndType(Long userId, MarkType type);

    void deleteByMovieId(Long movieId);
}
