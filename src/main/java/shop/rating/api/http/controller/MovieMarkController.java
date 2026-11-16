package shop.rating.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.rating.api.http.MovieMarkHttpApi;
import shop.rating.api.http.dto.MarkedMovieResponse;
import shop.rating.api.http.mapper.MovieMarkWebMapper;
import shop.rating.internal.domain.model.MarkType;
import shop.rating.internal.domain.service.MovieMarkService;
import shop.shared.security.CurrentUser;

import java.util.List;

/** HTTP entry of the favourite / watched lists: implements the {@link MovieMarkHttpApi} contract and delegates. */
@RestController
public class MovieMarkController implements MovieMarkHttpApi {

    private final MovieMarkService movieMarkService;
    private final MovieMarkWebMapper mapper;
    private final CurrentUser currentUser;

    public MovieMarkController(MovieMarkService movieMarkService, MovieMarkWebMapper mapper,
                               CurrentUser currentUser) {
        this.movieMarkService = movieMarkService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public void markFavorite(Long movieId) {
        movieMarkService.mark(mapper.toDto(currentUser.id(), movieId, MarkType.FAVORITA));
    }

    @Override
    public void unmarkFavorite(Long movieId) {
        movieMarkService.unmark(mapper.toDto(currentUser.id(), movieId, MarkType.FAVORITA));
    }

    @Override
    public void markWatched(Long movieId) {
        movieMarkService.mark(mapper.toDto(currentUser.id(), movieId, MarkType.VISTA));
    }

    @Override
    public void unmarkWatched(Long movieId) {
        movieMarkService.unmark(mapper.toDto(currentUser.id(), movieId, MarkType.VISTA));
    }

    @Override
    public List<MarkedMovieResponse> listFavorites() {
        return mapper.toResponses(movieMarkService.listMarked(currentUser.id(), MarkType.FAVORITA));
    }

    @Override
    public List<MarkedMovieResponse> listWatched() {
        return mapper.toResponses(movieMarkService.listMarked(currentUser.id(), MarkType.VISTA));
    }
}
