package shop.shared.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ApiError(
        @JsonProperty("estado") int status,
        @JsonProperty("mensaje") String message,
        @JsonProperty("errores") Map<String, String> errors) {
}
