package shop.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import shop.catalog.api.facade.CatalogFacade;
import shop.rating.internal.application.usecase.ListMarkedMovies;
import shop.rating.internal.application.usecase.MarkMovie;
import shop.rating.internal.application.usecase.RateMovie;
import shop.rating.internal.application.usecase.UnmarkMovie;
import shop.rating.internal.domain.contract.MovieCatalog;
import shop.rating.internal.domain.repository.MovieMarkRepository;
import shop.rating.internal.domain.service.MovieMarkService;
import shop.rating.internal.domain.service.MovieMarkServiceImpl;
import shop.rating.internal.domain.repository.RatingRepository;
import shop.rating.internal.domain.service.RatingService;
import shop.rating.internal.domain.service.RatingServiceImpl;
import shop.rating.internal.persistence.adapter.MovieCatalogAdapter;
import shop.rating.internal.persistence.repository.JdbcMovieMarkRepository;
import shop.rating.internal.persistence.repository.JdbcRatingRepository;

@Configuration
class RatingModuleConfig {

    @Bean
    RatingRepository ratingRepository(JdbcClient jdbc) {
        return new JdbcRatingRepository(jdbc);
    }

    @Bean
    MovieMarkRepository movieMarkRepository(JdbcClient jdbc) {
        return new JdbcMovieMarkRepository(jdbc);
    }

    @Bean
    MovieCatalog ratingMovieCatalog(CatalogFacade catalog) {
        return new MovieCatalogAdapter(catalog);
    }

    @Bean
    RatingService ratingService(RatingRepository ratings, MovieCatalog ratingMovieCatalog) {
        return new RatingServiceImpl(ratings, ratingMovieCatalog);
    }

    @Bean
    MovieMarkService movieMarkService(MovieMarkRepository marks, MovieCatalog ratingMovieCatalog) {
        return new MovieMarkServiceImpl(marks, ratingMovieCatalog);
    }

    @Bean
    RateMovie rateMovie(RatingService ratingService) {
        return new RateMovie(ratingService);
    }

    @Bean
    MarkMovie markMovie(MovieMarkService movieMarkService) {
        return new MarkMovie(movieMarkService);
    }

    @Bean
    UnmarkMovie unmarkMovie(MovieMarkService movieMarkService) {
        return new UnmarkMovie(movieMarkService);
    }

    @Bean
    ListMarkedMovies listMarkedMovies(MovieMarkService movieMarkService) {
        return new ListMarkedMovies(movieMarkService);
    }
}
