package shop.rating.api.http.controller;

import org.springframework.web.bind.annotation.RestController;
import shop.rating.api.http.RatingHttpApi;
import shop.rating.api.http.dto.RatingRequest;
import shop.rating.api.http.dto.RatingResponse;
import shop.rating.api.http.mapper.RatingWebMapper;
import shop.rating.internal.domain.service.RatingService;
import shop.shared.security.CurrentUser;

/** HTTP entry of ratings: implements the {@link RatingHttpApi} contract and delegates. */
@RestController
public class RatingController implements RatingHttpApi {

    private final RatingService ratingService;
    private final RatingWebMapper mapper;
    private final CurrentUser currentUser;

    public RatingController(RatingService ratingService, RatingWebMapper mapper, CurrentUser currentUser) {
        this.ratingService = ratingService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Override
    public RatingResponse rateMovie(Long movieId, RatingRequest request) {
        return mapper.toResponse(ratingService.rate(mapper.toDto(currentUser.id(), movieId, request)));
    }
}
