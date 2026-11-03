package shop.rating.internal.persistence.mapper;

import shop.rating.internal.persistence.entity.MovieMarkEntity;
import shop.rating.internal.persistence.entity.RatingEntity;

import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.model.MovieMark;
import shop.rating.internal.domain.model.Rating;

/** Converts between the framework-free domain model and the JPA entities. */
public final class RatingPersistenceMapper {

    private RatingPersistenceMapper() {
    }

    public static Rating toDomain(RatingEntity e) {
        var d = new Rating();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setMovieId(e.getMovieId());
        d.setScore(e.getScore());
        d.setComment(e.getComment());
        return d;
    }

    public static RatingEntity toEntity(Rating d) {
        var e = new RatingEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setMovieId(d.getMovieId());
        e.setScore(d.getScore());
        e.setComment(d.getComment());
        return e;
    }

    public static MovieMark toDomain(MovieMarkEntity e) {
        var d = new MovieMark();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setMovieId(e.getMovieId());
        d.setType(e.getType());
        return d;
    }

    public static MovieMarkEntity toEntity(MovieMark d) {
        var e = new MovieMarkEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setMovieId(d.getMovieId());
        e.setType(d.getType());
        return e;
    }
}
