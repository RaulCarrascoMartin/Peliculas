package shop.catalog.internal.persistence.mapper;

import shop.catalog.internal.persistence.entity.MovieEntity;

import shop.catalog.internal.domain.model.Movie;

/** Converts between the framework-free domain model and the JPA entities. */
public final class MoviePersistenceMapper {

    private MoviePersistenceMapper() {
    }

    public static Movie toDomain(MovieEntity e) {
        var d = new Movie();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setYear(e.getYear());
        d.setGenre(e.getGenre());
        d.setDirector(e.getDirector());
        d.setAverageRating(e.getAverageRating());
        d.setPrice(e.getPrice());
        d.setStock(e.getStock());
        return d;
    }

    public static MovieEntity toEntity(Movie d) {
        var e = new MovieEntity();
        e.setId(d.getId());
        e.setTitle(d.getTitle());
        e.setYear(d.getYear());
        e.setGenre(d.getGenre());
        e.setDirector(d.getDirector());
        e.setAverageRating(d.getAverageRating());
        e.setPrice(d.getPrice());
        e.setStock(d.getStock());
        return e;
    }
}
