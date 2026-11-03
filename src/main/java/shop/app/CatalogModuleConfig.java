package shop.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.catalog.api.facade.CatalogFacade;
import shop.catalog.internal.application.facade.CatalogFacadeImpl;
import shop.catalog.internal.application.usecase.CreateMovie;
import shop.catalog.internal.application.usecase.DeleteMovie;
import shop.catalog.internal.application.usecase.GetMovie;
import shop.catalog.internal.application.usecase.ReleaseStock;
import shop.catalog.internal.application.usecase.ReserveStock;
import shop.catalog.internal.application.usecase.SearchMovies;
import shop.catalog.internal.application.usecase.UpdateMovie;
import shop.catalog.internal.domain.repository.MovieRepository;
import shop.catalog.internal.domain.service.MovieService;
import shop.catalog.internal.domain.service.MovieServiceImpl;
import shop.catalog.internal.domain.service.StockService;
import shop.catalog.internal.domain.service.StockServiceImpl;
import shop.catalog.internal.persistence.jpa.MovieJpaRepository;
import shop.catalog.internal.persistence.repository.MovieRepositoryImpl;

@Configuration
class CatalogModuleConfig {

    @Bean
    MovieRepository movieRepository(MovieJpaRepository dao) {
        return new MovieRepositoryImpl(dao);
    }

    @Bean
    MovieService movieService(MovieRepository movies) {
        return new MovieServiceImpl(movies);
    }

    @Bean
    StockService stockService(MovieRepository movies) {
        return new StockServiceImpl(movies);
    }

    @Bean
    CreateMovie createMovie(MovieService movieService) {
        return new CreateMovie(movieService);
    }

    @Bean
    GetMovie getMovie(MovieService movieService) {
        return new GetMovie(movieService);
    }

    @Bean
    SearchMovies searchMovies(MovieService movieService) {
        return new SearchMovies(movieService);
    }

    @Bean
    UpdateMovie updateMovie(MovieService movieService) {
        return new UpdateMovie(movieService);
    }

    @Bean
    DeleteMovie deleteMovie(MovieService movieService) {
        return new DeleteMovie(movieService);
    }

    @Bean
    ReserveStock reserveStock(StockService stockService) {
        return new ReserveStock(stockService);
    }

    @Bean
    ReleaseStock releaseStock(StockService stockService) {
        return new ReleaseStock(stockService);
    }

    /** Public face of the module: the only bean other modules reach (through {@link CatalogFacade}). */
    @Bean
    CatalogFacade catalogFacade(MovieService movieService, ReserveStock reserveStock, ReleaseStock releaseStock) {
        return new CatalogFacadeImpl(movieService, reserveStock, releaseStock);
    }
}
