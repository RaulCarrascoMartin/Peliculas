package shop.authentication.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @JsonProperty("contrasena") @NotBlank String password) {
}
