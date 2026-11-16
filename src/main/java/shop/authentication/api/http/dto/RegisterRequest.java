package shop.authentication.api.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @JsonProperty("contrasena") @NotBlank @Size(min = 8, max = 72) String password) {
}
