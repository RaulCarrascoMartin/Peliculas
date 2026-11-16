package shop.rating.api.http.controller;

import shop.rating.api.http.dto.RatingRequest;
import shop.rating.api.http.dto.RatingResponse;

import shop.shared.security.CurrentUser;
import jakarta.validation.Valid;
import shop.rating.internal.application.usecase.RateMovie;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas/{movieId}/valoracion")
public class RatingController {

    private final RateMovie rateMovie;
    private final CurrentUser currentUser;

    public RatingController(RateMovie rateMovie, CurrentUser currentUser) {
        this.rateMovie = rateMovie;
        this.currentUser = currentUser;
    }

    @PutMapping
    public RatingResponse rateMovie(@PathVariable Long movieId, @Valid @RequestBody RatingRequest request) {
        return RatingResponse.from(rateMovie.execute(currentUser.id(), movieId, request.score(), request.comment()));
    }
}
