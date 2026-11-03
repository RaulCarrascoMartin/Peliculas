package shop.rating.internal.web;

import jakarta.validation.Valid;
import shop.rating.internal.usecase.RateMovie;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peliculas/{movieId}/valoracion")
public class RatingController {

    private final RateMovie rateMovie;

    public RatingController(RateMovie rateMovie) {
        this.rateMovie = rateMovie;
    }

    @PutMapping
    public RatingResponse rateMovie(@PathVariable Long movieId, @Valid @RequestBody RatingRequest request) {
        return RatingResponse.from(rateMovie.execute(movieId, request.score(), request.comment()));
    }
}
